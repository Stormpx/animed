package org.stormpx.animed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 用真实的 bgm 搜索响应（src/test/resources/bgm 下，2026-11 抓取）做回归，
 * 覆盖 "罗梅莉亚战记" 这组典型歧义：本体 / 第2部分 / 名字相近的罗小黑战记。
 */
class BgmSubjectResolverTest {

    private val resolver = BgmSubjectResolver()

    private fun candidates(vararg resources: String): List<BgmSubjectResolver.Candidate> {
        return resources.flatMap { resource ->
            val stream = javaClass.getResourceAsStream("/bgm/$resource")
                ?: error("missing test resource: bgm/$resource")
            val body = stream.use { Json.decodeFromString<JsonObject>(String(it.readAllBytes())) }
            (body["data"] as JsonArray).filterIsInstance<JsonObject>()
                .mapNotNull { BgmApi.toInfo(it) }
                .map { it.toCandidate() }
        }
    }

    private fun resolve(
        titles: List<String>,
        episode: Double?,
        pubDate: LocalDateTime?,
        vararg resources: String,
    ): BgmSubjectResolver.Decision {
        return resolver.resolve(titles, episode, pubDate, candidates(*resources))
    }

    private fun resolvedId(decision: BgmSubjectResolver.Decision): Int {
        return (decision as? BgmSubjectResolver.Decision.Resolved)?.best?.candidate?.id
            ?: error("expected a resolved decision, but got $decision")
    }

    @Test
    fun testPickBaseSeasonInsteadOfSecondPart() {
        // "罗梅莉亚战记 - 05"：候选里有本体(511936)、第2部分(717930)、罗小黑战记(13603/159725/...)
        val decision = resolve(
            listOf("罗梅莉亚战记"),
            episode = 5.0,
            pubDate = LocalDateTime.of(2026, 11, 9, 12, 0),
            "search-romelia.json"
        )
        assertEquals(511936, resolvedId(decision))
    }

    @Test
    fun testPickSecondPartWhenResourceSaysSo() {
        // "ロメリア戦記 第IIクール - 13"，连载于 2027 年 4 月
        val decision = resolve(
            listOf("ロメリア戦記 第IIクール"),
            episode = 13.0,
            pubDate = LocalDateTime.of(2027, 5, 10, 12, 0),
            "search-romelia.json"
        )
        assertEquals(717930, resolvedId(decision))
    }

    @Test
    fun testTraditionalChineseTitle() {
        val decision = resolve(
            listOf("羅梅莉亞戰記"),
            episode = 5.0,
            pubDate = LocalDateTime.of(2026, 11, 9, 12, 0),
            "search-romelia.json"
        )
        assertEquals(511936, resolvedId(decision))
    }

    @Test
    fun testRomajiVariantMatchesAlias() {
        // "[喵萌奶茶屋&LoliHouse] 罗梅莉亚战记 / Romelia Senki - 05" 解析出两个变体
        val decision = resolve(
            listOf("罗梅莉亚战记", "RomeliaSenki"),
            episode = 5.0,
            pubDate = LocalDateTime.of(2026, 11, 9, 12, 0),
            "search-romelia.json"
        )
        assertEquals(511936, resolvedId(decision))
    }

    @Test
    fun testSimilarButDifferentTitleIsRejected() {
        // 名字相近的罗小黑战记不该被选中：归一化相似度低于门槛
        val decision = resolve(
            listOf("罗梅莉亚战记"),
            episode = 5.0,
            pubDate = LocalDateTime.of(2026, 11, 9, 12, 0),
            "search-romelia.json"
        )
        val ranked = when (decision) {
            is BgmSubjectResolver.Decision.Resolved -> decision.ranked
            is BgmSubjectResolver.Decision.Ambiguous -> decision.ranked
            BgmSubjectResolver.Decision.NotFound -> emptyList()
        }
        assertTrue(ranked.none { it.candidate.id == 13603 }, "罗小黑战记 must not be a candidate of 罗梅莉亚战记")
    }

    @Test
    fun testEraMatchingPicksTheRightLuoXiaoHei() {
        // 2011 年的资源只可能是 2011 年开播的 13603
        val decision = resolve(
            listOf("罗小黑战记"),
            episode = 5.0,
            pubDate = LocalDateTime.of(2011, 4, 1, 12, 0),
            "search-luoxiaohei.json"
        )
        assertEquals(13603, resolvedId(decision))
    }

    @Test
    fun testWithoutPubDateTheExactNameWins() {
        // 没有发布时间时无法用年代排除派生作：靠名字分差把 2011 本体排在"学前篇"之前
        val decision = resolve(
            listOf("罗小黑战记"),
            episode = 5.0,
            pubDate = null,
            "search-luoxiaohei.json"
        )
        assertEquals(13603, resolvedId(decision))
        val ranked = (decision as BgmSubjectResolver.Decision.Resolved).ranked
        assertTrue(ranked.size > 1, "runner up should be visible for diagnostics")
    }

    @Test
    fun testNotFoundWhenNothingIsSimilar() {
        val decision = resolve(
            listOf("完全不存在的作品名"),
            episode = 1.0,
            pubDate = LocalDateTime.of(2026, 11, 9, 12, 0),
            "search-romelia.json"
        )
        assertTrue(decision is BgmSubjectResolver.Decision.NotFound, "expected not found, but got $decision")
    }

}
