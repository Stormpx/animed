package org.stormpx.animed

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.animed.download.Downloader
import org.stormpx.parser.TitleParser
import org.stormpx.rss.Channel
import org.stormpx.rss.Item
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Objects
import java.util.concurrent.*
import kotlin.io.path.exists

open class Worker(
    dataPath: Path,
    private val animeConfig: AnimeConfig,
    private val context: AnimedContext,

    ) {
    companion object {
        private val json= Json{

            prettyPrint=true
        }
        private val logger: Logger = LoggerFactory.getLogger(Worker::class.java)

        /** 已经超过 bgm 总集数时，RSS 改为每 24 小时才看一次 */
        private const val BEYOND_BGM_EPS_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000

        /** bgm 尚未给出集数时，最多 24 小时去问一次，避免每个刷新周期都打接口 */
        private const val BGM_EPS_PROBE_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000
    }

    class WorkerException : RuntimeException {
        constructor(message: String?) : super(message)
        constructor(message: String?,cause: Throwable?) : super(message,cause)
    }

    @kotlinx.serialization.Serializable
    data class AnimeData(var id: String, @SerialName("chapter")var episode: Double?=null,
                         val entrys:ArrayList<EntryInfo> = ArrayList(),
                         @SerialName("bgm_id") var bgmId: Int? = null,
                         @SerialName("bgm_eps") var bgmEps: Int? = null)
    @kotlinx.serialization.Serializable
    data class EntryInfo(val id:String?,val title:String,val link:String,val chapter:Double)


    private val path: Path = dataPath.resolve("${id()}.json")
    private val matchers: Array<Matcher>
    private var future: ScheduledFuture<*>? = null
    private val bgmResolver: BgmSubjectResolver = BgmSubjectResolver()

    /** 上一次真正去读 RSS 的时间，用来在超过 bgm 总集数后降频 */
    @Volatile
    private var lastFetchAt: Long = 0L

    /** 上一次去 bgm 查集数的时间（bgm 对新番/未定档条目经常一开始没有集数） */
    @Volatile
    private var lastBgmEpsProbe: Long = 0L


    init {
        try {
            matchers = Animed.INSTANCE.newMatchers(animeConfig.patterns, animeConfig.titles)
            if (matchers.isEmpty()) {
                throw WorkerException("anime '${animeConfig.id}' has neither 'titles' nor 'patterns' configured: no RSS item can ever match")
            }
        } catch (e: WorkerException) {
            throw e
        } catch (e: Exception) {
            throw WorkerException("unable to initialize matchers of anime '${animeConfig.id}': ${e.message ?: e.javaClass.simpleName}",e)
        }
    }



    protected open fun getAnimeData(): AnimeData {
        if (!Animed.INSTANCE.isSafe(path.parent)){
            throw IllegalStateException("refusing to access '$path': data directory '${path.parent}' is not locked by animed")
        }
        Files.createDirectories(path.parent)
        if (!path.exists()){
            return AnimeData(id(), -1.0, ArrayList())
        }

        return try {
            logger.info("reading downloaded episodes from '$path'")
            json.decodeFromStream(AnimeData.serializer(),Files.newInputStream(path))
        } catch (e:Exception) {
            logger.warn("unable to read downloaded episodes from '$path', starting from an empty state: ${e.message ?: e.javaClass.simpleName}",e)
            AnimeData(id(), -1.0, ArrayList())
        }
    }

    protected open fun saveAnimeData(anime:AnimeData){
        if (!Animed.INSTANCE.isSafe(path.parent)){
            throw IllegalStateException("refusing to save '$path': data directory '${path.parent}' is not locked by animed")
        }
        anime.entrys.sortWith { o1, o2 -> o2.chapter.compareTo(o1.chapter) }

        json.encodeToStream(anime,Files.newOutputStream(path,StandardOpenOption.CREATE,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING))
    }


    fun id(): String {
        return animeConfig.id
    }

    fun isSameId(otherId:String):Boolean{
        return Objects.equals(id(),otherId)
    }

    fun isOptionChange(otherAnimeConfig: AnimeConfig): Boolean {
        return isSameId(otherAnimeConfig.id) && !Objects.equals(animeConfig, otherAnimeConfig);
    }

    private fun logFailure(message: String, e: Exception) {
        if (e is WorkerException) {
            logger.error("$message: ${e.message ?: e.javaClass.simpleName}")
        } else {
            logger.error(message,e)
        }
    }

    private fun downloader(): Downloader {
        val downloaderId = animeConfig.downloader
        return context.getDownloader(downloaderId)
            ?: throw WorkerException("downloader '$downloaderId' is not configured: check the 'downloader' section of the config")
    }

    protected open fun getChannel(url: String): Channel {
        return Animed.INSTANCE.getChannel(url)
    }

    /** 可覆写以便测试不访问网络 */
    protected open fun searchBgmSubjects(title: String): List<BgmApi.BgmInfo> {
        return BgmApi.getInfo(title)
    }

    protected open fun getBgmSubject(id: Int): BgmApi.BgmInfo? {
        return BgmApi.getSubject(id)
    }


    /**
     * 确保 [AnimeData] 里持有 bgm 条目 id 与总集数。
     *
     * id 的优先级：config 里的 bgm_id > 自己保存的 bgm_id > 根据 RSS 条目标题搜索推断。
     * eps 一律通过详情接口获取：搜索接口对未定档/分割放送的条目经常返回 0。
     */
    protected open fun updateBgmInfo(animeData: AnimeData, channel: Channel) {
        var updated = false

        animeConfig.bgmId?.let { configured ->
            if (configured != animeData.bgmId) {
                logger.info("${id()}-> use bgm subject $configured from config (was: ${animeData.bgmId})")
                animeData.bgmId = configured
                // id 变了，总集数必须重新拉
                animeData.bgmEps = null
                lastBgmEpsProbe = 0L
                updated = true
            }
        }

        if (animeData.bgmId == null) {
            resolveBgmId(channel)?.let { resolvedId ->
                animeData.bgmId = resolvedId
                updated = true
            }
        }

        if (animeData.bgmEps == null) {
            val bgmId = animeData.bgmId
            if (bgmId == null) {
                logger.info("${id()}-> bgm subject is still unresolved, total episode is unknown")
            } else if (isBgmEpsProbeThrottled()) {
                logger.debug("${id()}-> bgm subject $bgmId still has no episode count, skip probing until next window")
            } else {
                lastBgmEpsProbe = System.currentTimeMillis()
                try {
                    val subject = getBgmSubject(bgmId)
                    val eps = subject?.eps
                    if (eps != null && eps > 0) {
                        animeData.bgmEps = eps
                        updated = true
                        logger.info("${id()}-> bgm subject $bgmId has $eps episodes")
                    } else {
                        logger.warn("${id()}-> bgm subject $bgmId has no known episode count yet")
                    }
                } catch (e: Exception) {
                    logFailure("${id()}-> failed to read bgm subject $bgmId", e)
                }
            }
        }

        if (updated) {
            try {
                saveAnimeData(animeData)
            } catch (e: Exception) {
                logFailure("${id()}-> failed to save bgm info", e)
            }
        }
    }

    private fun resolveBgmId(channel: Channel): Int? {
        val item = channel.items?.firstOrNull { item ->
            val title = item.title?.trim() ?: return@firstOrNull false
            matchers.any { it.match(title).match }
        }
        if (item == null) {
            logger.info("${id()}-> no matched rss item yet, bgm subject is left unresolved")
            return null
        }
        val itemTitle = item.title?.trim()
        if (itemTitle.isNullOrBlank()) {
            return null
        }
        val parsedTitle = try {
            TitleParser().parse(itemTitle)
        } catch (e: Exception) {
            logFailure("${id()}-> failed to parse rss item title '$itemTitle'", e)
            return null
        }
        val titles = parsedTitle.animeTitle()?.filter { it.isNotBlank() } ?: emptyList()
        if (titles.isEmpty()) {
            logger.info("${id()}-> rss item '$itemTitle' contains no usable anime title")
            return null
        }

        val candidates = LinkedHashMap<Int, BgmApi.BgmInfo>()
        for (title in titles) {
            try {
                searchBgmSubjects(title).forEach { candidates.putIfAbsent(it.id, it) }
            } catch (e: Exception) {
                logFailure("${id()}-> bangumi subject search failed for '$title'", e)
            }
        }
        if (candidates.isEmpty()) {
            logger.info("${id()}-> no bangumi subject found for rss item '$itemTitle'")
            return null
        }

        return when (val decision = bgmResolver.resolve(titles, parsedTitle.episode, item.pubDate, candidates.values.map { it.toCandidate() })) {
            is BgmSubjectResolver.Decision.Resolved -> {
                val candidate = decision.best.candidate
                logger.info("${id()}-> resolved bgm subject from rss item '$itemTitle': id=${candidate.id} names=${candidate.names}")
                candidate.id
            }

            is BgmSubjectResolver.Decision.Ambiguous -> {
                logger.warn(
                    "${id()}-> can not determine the bgm subject from rss item '{}', bgm id left unset (set 'bgm_id' in config to fix):\n    {}",
                    itemTitle,
                    decision.ranked.take(3).joinToString("\n    ") { it.report() }
                )
                context.notice(
                    AnimedContext.AnimeMessage(
                        animeConfig.targets ?: emptyArray(),
                        id(),
                        "无法确定《${id()}》对应的 bangumi 条目",
                        "RSS 条目: $itemTitle<br/>候选:<br/>" +
                            decision.ranked.take(3).joinToString("<br/>") { it.report() } +
                            "<br/>如需正确集数，请在配置里手动填写 bgm_id。"
                    )
                )
                null
            }

            BgmSubjectResolver.Decision.NotFound -> {
                logger.info("${id()}-> no bangumi subject matches rss item '$itemTitle'")
                null
            }
        }
    }

    /** 已下载的集数已经超过 bgm 总集数（分割放送/连续编号/数据填错），后续没必要频繁刷新 */
    private fun isBeyondBgmEps(animeData: AnimeData): Boolean {
        val eps = animeData.bgmEps ?: return false
        val episode = animeData.episode ?: return false
        return episode > eps
    }

    private fun isBgmEpsProbeThrottled(): Boolean {
        val last = lastBgmEpsProbe
        return last > 0 && System.currentTimeMillis() - last < BGM_EPS_PROBE_INTERVAL_MILLIS
    }

    private fun tryGetTorrent(item:Item):String?{

        val enclosure = item.enclosure
        if (enclosure !=null){
            //normal case
            if (enclosure.url!=null&& enclosure.url.startsWith("magnet")){
                return enclosure.url
            }
            //acg.rip case
            if (enclosure.type!=null&&enclosure.type.contains("bittorrent")){
                return enclosure.url
            }
        }
        //nyaa.si case
        if (item.link?.contains("torrent") == true){
            return item.link
        }


        return null;

    }

    private fun tryDownloadFailureChapter(data:AnimeData) : Boolean{
        var update=false;
        val entrys = data.entrys
            .filter { it.id == null }
            .mapIndexedNotNull{idx,it->
                logger.info("detected chapter ${it.title} is not successfully downloaded, try download again")
                try {
                    val id = downloader().downloadUri(it.link,animeConfig.downloadPath())
                    logger.info("${id()}-> start download chapter ${it.chapter} ${it.title}")

                    return@mapIndexedNotNull idx to EntryInfo(id,it.title,it.link,it.chapter)
                } catch (e: Exception) {
                    logFailure("${id()}-> failed to retry chapter ${it.chapter} '${it.title}'",e)
                    null
                }
            }

        entrys.forEach{
            update=true;
            data.entrys[it.first] = it.second
        }
        if (update){
            val content =entrys.map { it.second }.joinToString { it.title }
            context.notice(AnimedContext.AnimeMessage(animeConfig.targets?: emptyArray(),id(),"尝试重新下载${data.id}之前下载失败的剧集",content))
        }

        return update
    }

    fun start() {
        try {
            val animeData = getAnimeData()

            try {
                val animeDataUpdated = tryDownloadFailureChapter(animeData)
                if (animeDataUpdated){
                    saveAnimeData(animeData)
                }
            } catch (e: Exception) {
                logFailure("worker '${animeData.id}' failed to retry previously failed chapters",e)
            }

            animeConfig.finalEpisode?.let {finalEp->
                animeData.episode?.let { ep->
                    if (ep.compareTo(finalEp)>=0){
                        return@start
                    }
                }
            }

            // 已下载集数超过 bgm 总集数（分割放送/连续编号/数据不准）：改成最多 24 小时刷一次
            if (isBeyondBgmEps(animeData)) {
                val elapsed = System.currentTimeMillis() - lastFetchAt
                if (lastFetchAt > 0 && elapsed < BEYOND_BGM_EPS_INTERVAL_MILLIS) {
                    logger.info(
                        "${id()}-> episode ${animeData.episode} is beyond bgm eps ${animeData.bgmEps}, next rss check in ${TimeUnit.MILLISECONDS.toMinutes(BEYOND_BGM_EPS_INTERVAL_MILLIS - elapsed)} min"
                    )
                    return
                }
            }

            val channel = try {
                getChannel(animeConfig.rss)
            } catch (e: Exception) {
                throw WorkerException("unable to read RSS feed '${animeConfig.rss}' of anime '${id()}': ${e.message}")
            }
            lastFetchAt = System.currentTimeMillis()

            try {
                updateBgmInfo(animeData, channel)
            } catch (e: Exception) {
                logFailure("${id()}-> failed to update bgm info", e)
            }

            val newChapterPairs = channel.items
                .asSequence()
                .filter { !it.title.isNullOrBlank() }
                .map {
                    it to (matchers.map { matcher -> matcher.match(it.title.trim()) }.firstOrNull { r -> r.match } ?: MatchResult(false, null))
                }
                .filter { it.second.match }
                .filter { it.second.chapter()> animeConfig.startEpisode }
                .filter { it.second.chapter() > (animeData.episode ?: -1.0) }
                .distinctBy { it.second.chapter }
                .toList()

            if (newChapterPairs.isEmpty()){
                logger.info("${id()} no new chapter detected")
                return
            }
            newChapterPairs.map { pair ->
                    val item = pair.first
                    val result = pair.second
                    var torrentUri: String? = null
                    try {
                        torrentUri = tryGetTorrent(item)
                            ?: throw WorkerException("RSS item '${item.title}' has no torrent or magnet link (link: '${item.link}')")
                        val id = downloader().downloadUri(torrentUri,animeConfig.downloadPath())
                        logger.info("${id()}-> start download new chapter ${result.chapter} ${item.title}")
                        context.notice(AnimedContext.AnimeMessage(animeConfig.targets?: emptyArray(),id(),item.title,item.description))
                        return@map pair to EntryInfo(id,item.title,torrentUri,result.chapter())
                    } catch (e: Exception) {
                        logFailure("${id()}-> failed to download new chapter ${result.chapter()} '${item.title}'",e)
                        return@map pair to EntryInfo(null,item.title,torrentUri ?: item.link.orEmpty(),result.chapter())
                    }
                }
                .stream()
                .peek{ animeData.entrys.add(it.second) }
                .map { it.first.second }
                .max { o1, o2 -> o1.chapter().compareTo(o2.chapter()) }
                .ifPresent {
                    animeData.episode=it.chapter
                    saveAnimeData(animeData)
                }
        } catch (e: Exception) {
            logFailure("worker '${id()}' failed to refresh RSS feed '${animeConfig.rss}'",e)
        }

    }

    fun schedule() {
        cancel()
        future = context.scheduler().scheduleAtFixedRate(
            { start() },
            animeConfig.refreshInterval,
            animeConfig.refreshInterval,
            TimeUnit.SECONDS
        ) as ScheduledFuture<*>
    }

    fun cancel() {
        future?.let {
            if (!it.isCancelled) {
                it.cancel(false);
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Worker

        if (animeConfig.id != other.animeConfig.id) return false

        return true
    }

    override fun hashCode(): Int {
        return animeConfig.id.hashCode()
    }


}