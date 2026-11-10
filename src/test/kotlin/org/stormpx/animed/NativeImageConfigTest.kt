package org.stormpx.animed

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 繁简/日文归一化依赖 opencc4j 的两样东西，两者都不会自动进 native image：
 *
 * 1. 字典是 classpath 资源，需要 build.gradle.kts 里的 `data/dictionary/.*` 模式（见 resource-config）
 * 2. opencc4j 内部用 `Class.newInstance()` 创建下面几个类，原生镜像必须登记反射元数据，
 *    否则第一次调用 ZhJpConverterUtil 就会抛异常（代码会退化成不做转换，匹配质量下降）
 *
 * 这两条都靠配置维持，所以用测试锁住，避免以后有人换依赖或清理配置时静默失效。
 */
class NativeImageConfigTest {

    private val dictionaries = listOf(
        "JPVariants.txt",
        "JPShinjitaiCharacters.txt",
        "JPShinjitaiPhrases.txt",
        "STPhrases.txt",
        "TSPhrases.txt",
    )

    private val reflectivelyInstantiated = listOf(
        "com.github.houbb.opencc4j.support.data.impl.STPhraseData",
        "com.github.houbb.opencc4j.support.data.impl.TSCharData",
        "com.github.houbb.opencc4j.support.data.impl.TSPhraseData",
        "com.github.houbb.opencc4j.support.datamap.impl.DataMapDefault",
        "com.github.houbb.opencc4j.support.segment.impl.FastForwardSegment",
    )

    @Test
    fun testDictionariesAreOnClasspath() {
        for (name in dictionaries) {
            val stream = javaClass.getResourceAsStream("/data/dictionary/$name")
            assertNotNull(stream, "opencc4j dictionary '$name' must stay on the classpath")
            stream.use { it.readBytes() }
        }
    }

    @Test
    fun testOpenccClassesAreRegisteredForReflection() {
        val stream = javaClass.getResourceAsStream("/META-INF/native-image/org.stormpx/animed/reflect-config.json")
        assertNotNull(stream, "native image reflect-config.json must stay on the classpath")
        val entries = stream.use { Json.decodeFromString<JsonArray>(String(it.readAllBytes())) }
            .filterIsInstance<JsonObject>()
            .mapNotNull { it["name"]?.let { name -> name.toString().trim('"') } }
            .toSet()

        for (name in reflectivelyInstantiated) {
            assertTrue(name in entries, "$name is instantiated reflectively by opencc4j and must be in reflect-config.json")
        }
    }

}
