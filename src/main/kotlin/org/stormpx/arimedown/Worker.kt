package org.stormpx.arimedown

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.arimedown.download.Downloader
import org.stormpx.rss.Item
import org.stormpx.rss.RSSReader
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Objects
import java.util.concurrent.*
import kotlin.io.path.exists

class Worker(
    dataPath: Path,
    private val animeConfig: AnimeConfig,
    private val context: AnimedContext,

    ) {
    companion object {
        private val json= Json{

            prettyPrint=true
        }
        private val logger: Logger = LoggerFactory.getLogger(Worker::class.java)
    }

    class WorkerException : RuntimeException {
        constructor(message: String?) : super(message)
        constructor(message: String?,cause: Throwable?) : super(message,cause)
    }

    @kotlinx.serialization.Serializable
    data class AnimeData(var id: String, var chapter: Double?=null,
                         val entrys:ArrayList<EntryInfo> = ArrayList())
    @kotlinx.serialization.Serializable
    data class EntryInfo(val id:String,val title:String,val link:String,val chapter:Double)


    private val path: Path = dataPath.resolve("${id()}.json")
    private val matchers: Array<Matcher>
    private var future: ScheduledFuture<*>? = null


    init {
        try {
            assert(animeConfig.rules.isNotEmpty())
            matchers = buildMatcher(animeConfig.rules)
        } catch (e: Exception) {
            throw WorkerException(e.message,e)
        }
    }

    private fun buildMatcher(rules: Array<String>): Array<Matcher> {
        return rules.map { ChapterMatcher(it) }.toTypedArray()
    }

    private fun getAnimeData(): AnimeData {
        Files.createDirectories(path.parent)
        if (!path.exists()){
            return AnimeData(id(), -1.0, ArrayList())
        }

        return try {
            logger.info("try read $path")
            json.decodeFromStream(AnimeData.serializer(),Files.newInputStream(path))
        } catch (e:Exception) {
            logger.warn("$path already exists but failure when try to read data.");
            AnimeData(id(), -1.0, ArrayList())
        }
    }

    private fun saveAnimeData(anime:AnimeData){
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
        if (item.link.contains("torrent")){
            return item.link
        }


        return null;

    }

    fun start() {
        try {
            val animeData = getAnimeData()

            val response =
                Http.client.send(
                    HttpRequest.newBuilder().GET().uri(URI.create(animeConfig.rss)).build(),
                    BodyHandlers.ofInputStream()
                )
            logger.info("request ${animeConfig.rss} status_code = ${response.statusCode()}")
            if (response.statusCode() != 200) {
                throw RuntimeException("")
            }
            val channel = RSSReader(response.body()).read()


            val newChapterPairs = channel.items
                .asSequence()
                .map {
                    it to (matchers.map { matcher -> matcher.match(it.title) }.firstOrNull { r -> r.match } ?: MatchResult(false, null))
                }
                .filter { it.second.match }
                .filter { it.second.chapter()> animeConfig.startChapter }
                .filter { it.second.chapter() > (animeData.chapter ?: -1.0) }
                .distinctBy { it.second.chapter }
                .toList()

            if (newChapterPairs.isEmpty()){
                logger.info("${id()} no new chapter detected")
                return
            }
            newChapterPairs.mapNotNull {
                    val item = it.first
                    val result = it.second
                    try {
                        val torrentUri = tryGetTorrent(item) ?: throw WorkerException("torrent uri not found.")
                        val downloader = context.getDownloader(animeConfig.downloader)
                            ?: throw WorkerException("downloader [${animeConfig.downloader}] not found.")

                        val id = downloader.downloadUri(torrentUri,animeConfig.downloadPath)
                        logger.info("${id()}-> start download new chapter ${item.title}")
                        context.notice(AnimedContext.AnimeMessage(animeConfig.targets?: emptyArray(),id(),item.title,item.description))
                        return@mapNotNull it to EntryInfo(id,item.title,torrentUri,result.chapter())
                    } catch (e: Exception) {
                        logger.error("${id()}-> an exception occurred while downloading a new chapter ${item.title} : ${e.message}")
                        if (e !is WorkerException){
                            logger.error("",e)
                        }

                        null
                    }
                }
                .stream()
                .peek{ animeData.entrys.add(it.second) }
                .map { it.first.second }
                .max { o1, o2 -> o1.chapter().compareTo(o2.chapter()) }
                .ifPresent {
                    animeData.chapter=it.chapter
                    saveAnimeData(animeData)
                }
        } catch (e: Exception) {
            logger.error(e.message?: "execute failed")
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