package org.stormpx.animed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import org.stormpx.rss.Channel
import org.stormpx.rss.Enclosure
import org.stormpx.rss.Item
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkerBgmInfoTest {

    private val romeliaFixture = "/bgm/search-romelia.json"
    private val luoxiaoheiFixture = "/bgm/search-luoxiaohei.json"

    private fun fixture(resource: String): List<BgmApi.BgmInfo> {
        val stream = javaClass.getResourceAsStream(resource) ?: error("missing test resource: $resource")
        val body = stream.use { Json.decodeFromString<JsonObject>(String(it.readAllBytes())) }
        return (body["data"] as JsonArray).filterIsInstance<JsonObject>().mapNotNull { BgmApi.toInfo(it) }
    }

    private fun channel(vararg titles: Pair<String, LocalDateTime?>): Channel {
        val channel = Channel()
        channel.items = titles.map { (title, pubDate) ->
            Item()
                .setTitle(title)
                .setLink("https://example.invalid/$title")
                .setEnclosure(Enclosure("magnet:?xt=urn:btih:${title.hashCode()}", 0L, "application/x-bittorrent"))
                .setPubDate(pubDate)
        }
        return channel
    }

    private class TestWorker(
        dataPath: Path,
        animeConfig: AnimeConfig,
        context: AnimedContext,
        var data: Worker.AnimeData,
        private val channel: Channel,
        private val subjects: (String) -> List<BgmApi.BgmInfo>,
        private val subjectById: (Int) -> BgmApi.BgmInfo?,
    ) : Worker(dataPath, animeConfig, context) {

        var channelFetches = 0
        var saved = 0

        override fun getAnimeData(): AnimeData = data

        override fun saveAnimeData(anime: AnimeData) {
            data = anime
            saved++
        }

        override fun getChannel(url: String): Channel {
            channelFetches++
            return channel
        }

        override fun searchBgmSubjects(title: String): List<BgmApi.BgmInfo> = subjects(title)

        override fun getBgmSubject(id: Int): BgmApi.BgmInfo? = subjectById(id)
    }

    private fun worker(
        bgmId: Int? = null,
        data: Worker.AnimeData = Worker.AnimeData("romelia", -1.0, ArrayList()),
        channel: Channel,
        subjects: (String) -> List<BgmApi.BgmInfo> = { fixture(romeliaFixture) },
        subjectById: (Int) -> BgmApi.BgmInfo? = { id -> BgmApi.BgmInfo(id, "罗梅莉亚战记", listOf("罗梅莉亚战记"), 12) },
        finalEpisode: Double? = null,
    ): TestWorker {
        val config = AnimeConfig(
            id = "romelia",
            bgmId = bgmId,
            rss = "https://example.invalid/feed.xml",
            patterns = arrayOf("[Nekomoe kissaten&LoliHouse] 羅梅莉亞戰記 - #ep# [WebRip 1080p HEVC-10bit AAC][CHT]"),
            startEpisode = -1.0,
            finalEpisode = finalEpisode,
            refreshInterval = 3600,
            downloader = "mock",
            downloadPath = "/download",
        )
        return TestWorker(Path.of("build/tmp/worker-bgm-test"), config, MockAnimedContext(), data, channel, subjects, subjectById)
    }

    private fun episodeTitle(episode: Int) =
        "[Nekomoe kissaten&LoliHouse] 羅梅莉亞戰記 - %02d [WebRip 1080p HEVC-10bit AAC][CHT]".format(episode)

    @Test
    fun testResolveBgmIdAndEpsWhenBothAreEmpty() {
        val worker = worker(
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
        )

        worker.start()

        assertEquals(511936, worker.data.bgmId)
        assertEquals(12, worker.data.bgmEps)
        assertTrue(worker.saved > 0, "bgm info must be persisted into the anime data")
    }

    @Test
    fun testConfigBgmIdWinsAndEpsComesFromDetail() {
        val worker = worker(
            bgmId = 717930,
            channel = channel(episodeTitle(5) to LocalDateTime.of(2027, 5, 10, 12, 0)),
            subjects = { error("search must not be called when config already has bgm_id") },
            subjectById = { id -> BgmApi.BgmInfo(id, "罗梅莉亚战记 第2部分", listOf("罗梅莉亚战记 第2部分"), 12) },
        )

        worker.start()

        assertEquals(717930, worker.data.bgmId)
        assertEquals(12, worker.data.bgmEps)
    }

    @Test
    fun testSavedBgmIdIsKeptAndOnlyEpsIsCompleted() {
        val data = Worker.AnimeData("romelia", 5.0, ArrayList(), bgmId = 511936)
        val worker = worker(
            data = data,
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = { error("search must not be called when bgm id is already saved") },
        )

        worker.start()

        assertEquals(511936, worker.data.bgmId)
        assertEquals(12, worker.data.bgmEps)
    }

    @Test
    fun testAmbiguousCandidatesLeaveBgmIdUnset() {
        // 两个候选名字完全一样、又都没有平台/日期/集数信息可以区分：不猜，交给人手填
        val worker = worker(
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = {
                listOf(
                    BgmApi.BgmInfo(1, "罗梅莉亚战记", listOf("罗梅莉亚战记"), null),
                    BgmApi.BgmInfo(2, "罗梅莉亚战记", listOf("罗梅莉亚战记"), null),
                )
            },
        )

        worker.start()

        assertNull(worker.data.bgmId)
        assertNull(worker.data.bgmEps)
        // 推断失败不影响正常下载流程
        assertEquals(5.0, worker.data.episode)
    }

    @Test
    fun testUnrelatedSearchResultIsRejected() {
        // 搜索只返回了名字相近的"罗小黑战记"，不该被当成《罗梅莉亚战记》
        val worker = worker(
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = { fixture(luoxiaoheiFixture) },
        )

        worker.start()

        assertNull(worker.data.bgmId)
        assertNull(worker.data.bgmEps)
        assertEquals(5.0, worker.data.episode)
    }

    @Test
    fun testNoBgmInfoWithoutMatchedItem() {
        val worker = worker(
            channel = channel("完全不相关的剧集 - 05" to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = { error("search must not run when no rss item matches") },
        )

        worker.start()

        assertNull(worker.data.bgmId)
        assertEquals(0, worker.saved)
    }

    @Test
    fun testThrottleTo24HoursBeyondBgmEps() {
        val data = Worker.AnimeData("romelia", 13.0, ArrayList(), bgmId = 511936, bgmEps = 12)
        val worker = worker(
            data = data,
            // 让 worker 找不到新集数，只观察取 feed 的次数
            channel = channel(episodeTitle(13) to LocalDateTime.of(2027, 1, 8, 12, 0)),
            subjects = { emptyList() },
        )

        worker.start()
        assertEquals(1, worker.channelFetches, "first run must fetch the feed")

        worker.start()
        assertEquals(1, worker.channelFetches, "beyond bgm eps the feed must be checked at most once per 24h")
    }

    @Test
    fun testUnknownBgmEpsIsProbedAtMostOncePerDay() {
        // bgm 对新番/未定档条目经常一开始没有集数（例如 717930: eps=0, total_episodes=0）。
        // 重试是必要的，但不能每个刷新周期都去打接口。
        var probes = 0
        val data = Worker.AnimeData("romelia", 5.0, ArrayList(), bgmId = 511936)
        val worker = worker(
            data = data,
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = { emptyList() },
            subjectById = { probes++; null },
        )

        worker.start()
        assertEquals(1, probes, "unknown episode count must be probed once")

        worker.start()
        assertEquals(1, probes, "probing must be throttled to once per 24h")
        assertNull(worker.data.bgmEps)
    }

    @Test
    fun testNoThrottleWithinBgmEps() {
        val data = Worker.AnimeData("romelia", 5.0, ArrayList(), bgmId = 511936, bgmEps = 12)
        val worker = worker(
            data = data,
            channel = channel(episodeTitle(5) to LocalDateTime.of(2026, 11, 9, 12, 0)),
            subjects = { emptyList() },
        )

        worker.start()
        worker.start()

        assertEquals(2, worker.channelFetches)
    }

    @Test
    fun testAnimeDataSerializationIsBackwardCompatible() {
        val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

        val encoded = json.encodeToString(Worker.AnimeData.serializer(),
            Worker.AnimeData("romelia", 13.0, ArrayList(), bgmId = 511936, bgmEps = 12))
        assertTrue(encoded.contains("\"bgm_id\": 511936"), encoded)
        assertTrue(encoded.contains("\"bgm_eps\": 12"), encoded)

        // 旧版本的动画数据文件没有 bgm 字段，必须还能读
        val legacy = json.decodeFromString(Worker.AnimeData.serializer(),
            """{"id":"romelia","chapter":3.0,"entrys":[]}""")
        assertNull(legacy.bgmId)
        assertNull(legacy.bgmEps)
        assertEquals(3.0, legacy.episode)
    }

}
