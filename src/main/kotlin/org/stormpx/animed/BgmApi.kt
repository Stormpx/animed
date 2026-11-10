package org.stormpx.animed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.LocalDate

class BgmApi {

    data class BgmInfo(
        val id: Int,
        val nameCn: String,
        val name: List<String>,
        val eps: Int?,
        val platform: String? = null,
        val date: LocalDate? = null,
    )

    companion object {

        private const val SUBJECT_SEARCH_URL: String = "https://api.bgm.tv/v0/search/subjects"
        private const val SUBJECT_URL: String = "https://api.bgm.tv/v0/subjects/"

        /** bgm 的 api 文档要求带可识别的 User-Agent */
        private const val USER_AGENT: String = "stormpx/animed (https://github.com/stormpx/animed)"

        fun searchAnime(keyword: String): List<JsonElement> {
            val body = JsonObject(
                mapOf(
                    "keyword" to JsonPrimitive(keyword),
                    "filter" to JsonObject(
                        mapOf(
                            "type" to JsonArray(listOf(JsonPrimitive(2)))
                        )
                    )
                )
            )
            val publisher = HttpRequest.BodyPublishers.ofString(Json.encodeToString(body))
            val response =
                Http.client.send(
                    HttpRequest.newBuilder()
                        .POST(publisher)
                        .uri(URI.create(SUBJECT_SEARCH_URL))
                        .header("content-type", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofMinutes(1))
                        .build(),
                    BodyHandlers.ofInputStream()
                )

            if (response.statusCode() != 200) {
                response.body().close()
                throw IllegalStateException("bangumi subject search for '$keyword' failed: HTTP ${response.statusCode()}")
            }

            response.body().use {
                val value = Json.decodeFromStream<JsonObject>(it)
                return value["data"] as? JsonArray ?: JsonArray(emptyList())
            }
        }

        fun getInfo(keyword: String): List<BgmInfo> {
            return searchAnime(keyword)
                .filterIsInstance<JsonObject>()
                .mapNotNull { toInfo(it) }
        }

        /**
         * 按 id 取条目详情。
         * 搜索接口的 eps/total_episodes 经常是 0（例如分晩第2部分），详情接口才给真实集数，
         * 所以确定 bgmEps 必须走这个接口。
         */
        fun getSubject(id: Int): BgmInfo? {
            val response = Http.client.send(
                HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(SUBJECT_URL + id))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofMinutes(1))
                    .build(),
                BodyHandlers.ofInputStream()
            )
            if (response.statusCode() != 200) {
                response.body().close()
                throw IllegalStateException("bangumi subject $id failed: HTTP ${response.statusCode()}")
            }
            return response.body().use { toInfo(Json.decodeFromStream<JsonObject>(it)) }
        }

        /**
         * 搜索结果的单条 subject -> [BgmInfo]。
         * eps 为 0 或缺失时退到 total_episodes，仍然拿不到就置 null（表示"未知"，不是"0 集"）。
         */
        fun toInfo(subject: JsonObject): BgmInfo? {
            val id = subject.int("id") ?: return null
            val name = subject.string("name").orEmpty()
            val nameCn = subject.string("name_cn").orEmpty()
            val infobox = subject["infobox"] as? JsonArray

            val names = LinkedHashSet<String>()
            if (name.isNotBlank()) names.add(name)
            if (nameCn.isNotBlank()) names.add(nameCn)
            infobox?.filterIsInstance<JsonObject>()?.forEach { info ->
                when (info.string("key")) {
                    "中文名" -> names.addAll(info.valueStrings())
                    "别名", "日文名", "英文名" -> names.addAll(info.valueStrings())
                }
            }

            val eps = subject.int("eps")?.takeIf { it > 0 }
//                ?: subject.int("total_episodes")?.takeIf { it > 0 }

            return BgmInfo(
                id = id,
                nameCn = nameCn,
                name = names.toList(),
                eps = eps,
                platform = subject.string("platform"),
                date = subject.string("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            )
        }

        private fun JsonObject.primitive(key: String): JsonPrimitive? {
            return (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }
        }

        private fun JsonObject.string(key: String): String? {
            return primitive(key)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
        }

        private fun JsonObject.int(key: String): Int? {
            return primitive(key)?.content?.trim()?.toIntOrNull()
        }

         /** infobox 的 value 可能是字符串，也可能是 [{v: "..."}] 数组 */
        private fun JsonObject.valueStrings(): List<String> {
            return when (val value = this["value"]) {
                is JsonPrimitive -> if (value is JsonNull || !value.isString) emptyList() else listOf(value.content)
                is JsonArray -> value.filterIsInstance<JsonObject>()
                    .mapNotNull { (it["v"] as? JsonPrimitive)?.takeIf { v -> v.isString }?.content }
                else -> emptyList()
            }
        }
    }

}
