package org.stormpx.animed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BgmApiTest {

    private fun subjects(resource: String): List<BgmApi.BgmInfo> {
        val stream = javaClass.getResourceAsStream("/bgm/$resource") ?: error("missing test resource: bgm/$resource")
        val body = stream.use { Json.decodeFromString<JsonObject>(String(it.readAllBytes())) }
        return (body["data"] as JsonArray).filterIsInstance<JsonObject>().mapNotNull { BgmApi.toInfo(it) }
    }

    @Test
    fun testAliasesAndMetadataAreExtracted() {
        val info = subjects("search-romelia.json").first { it.id == 511936 }
        assertEquals("罗梅莉亚战记", info.nameCn)
        assertEquals("TV", info.platform)
        assertEquals(LocalDate.of(2026, 10, 5), info.date)
        assertEquals(12, info.eps)
        // name + 中文名 + 别名（Romelia Senki 这类罗马字别名是匹配字幕组标题的关键）
        assertTrue(info.name.contains("ロメリア戦記"), "names should contain the original name: ${info.name}")
        assertTrue(info.name.contains("Romelia Senki"), "names should contain infobox aliases: ${info.name}")
    }

    @Test
    fun testUnlistedEpisodesAreUnknown() {
        // 717930 的搜索响应里 eps=0、total_episodes=0（话数填的是 "*"），
        // 只有 GET /v0/subjects/{id} 详情接口才给出 12。
        // 这里必须解析成 null（未知），不能当成 "0 集" 或 "12 集"。
        val info = subjects("search-romelia.json").first { it.id == 717930 }
        assertNull(info.eps)
        assertNull(info.date, "未定档的条目 date 应为 null 而不是抛异常")
        assertEquals("罗梅莉亚战记 第2部分", info.nameCn)
    }

    @Test
    fun testBlankNameCnIsNotAnAlias() {
        val info = subjects("search-luoxiaohei.json").first { it.id == 159725 }
        assertEquals("", info.nameCn)
        // name_cn 为空时不进别名表，但 infobox 的别名要收齐（含日文名/英文名）
        assertTrue(info.name.contains("罗小黑战记"))
        assertTrue(info.name.none { it.isBlank() }, "blank names must be dropped: ${info.name}")
        assertEquals("剧场版", info.platform)
    }

    @Test
    fun testMissingFieldsDoNotThrow() {
        val info = BgmApi.toInfo(JsonObject(mapOf("id" to JsonPrimitive(1))))
            ?: error("subject without id-less fields should still be usable")
        assertEquals(1, info.id)
        assertNull(info.eps)
        assertNull(info.platform)
        assertNull(info.date)
        assertTrue(info.name.isEmpty())
        assertNull(BgmApi.toInfo(JsonObject(emptyMap())), "subject without id must be dropped")
    }

}
