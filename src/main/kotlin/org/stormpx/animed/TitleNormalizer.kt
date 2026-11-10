package org.stormpx.animed

import com.github.houbb.opencc4j.util.ZhJpConverterUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.Normalizer

/**
 * 把 RSS 标题里的作品名和 bgm 里的作品名归一化到同一形态，让两边可以直接比较。
 *
 * 处理顺序：
 * 1. NFKC：全角转半角（ＲＯＭＥＬＩＡ -> ROMELIA、半角假名 ｶﾞ -> ガ），顺便统一罗马数字 Ⅱ -> II
 * 2. opencc4j 的繁简/日文字典：羅梅莉亞戰記 -> 罗梅莉亚战记、戦記 -> 战记、黒 -> 黑
 * 3. 片假名折叠成平假名：消掉同一个名字的假名写法差异
 * 4. 只保留字母与数字：空格、标点、★☆、×、- 一律删除
 *
 * 注意：归一化之后**不能**再依赖空格切词，TitleParser 已经去过空格，
 * 例如 "[喵萌奶茶屋] 罗梅莉亚战记 / Romelia Senki - 05" 的作品名是 "RomeliaSenki"。
 */
object TitleNormalizer {

    private val logger: Logger = LoggerFactory.getLogger(TitleNormalizer::class.java)

    @Volatile
    private var conversionBroken = false

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) {
            return ""
        }
        val nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        val simplified = toSimple(nfkc)
        val builder = StringBuilder(simplified.length)
        for (ch in simplified) {
            // 片假名 -> 平假名，其余不变
            val folded = if (ch in '\u30A1'..'\u30F6') ch - 0x60 else ch
            if (Character.isLetterOrDigit(folded)) {
                builder.append(folded.lowercaseChar())
            }
        }
        return builder.toString()
    }

    private fun toSimple(text: String): String {
        if (conversionBroken) {
            return text
        }
        return try {
            ZhJpConverterUtil.toSimple(text)
        } catch (e: Throwable) {
            // 字典资源缺失（例如 native image 没有打进 opencc4j 的 data/dictionary）时退化为不转换，
            // 归一化能力变弱但不会让整个 enrich 流程失败。
            conversionBroken = true
            logger.warn("traditional/simplified conversion is unavailable, title matching degrades to raw text: ${e.message ?: e.javaClass.simpleName}")
            text
        }
    }

    /**
     * 归一化后的相似度，取值 [0,1]，采用 1 - 编辑距离/最长长度。
     */
    fun similarity(left: String, right: String): Double {
        if (left == right) {
            return if (left.isEmpty()) 0.0 else 1.0
        }
        val longest = maxOf(left.length, right.length)
        if (longest == 0) {
            return 0.0
        }
        return 1.0 - levenshtein(left, right).toDouble() / longest
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (i in 1..left.length) {
            current[0] = i
            val leftChar = left[i - 1]
            for (j in 1..right.length) {
                val cost = if (leftChar == right[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }

}
