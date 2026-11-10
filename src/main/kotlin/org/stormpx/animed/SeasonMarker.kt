package org.stormpx.animed

/**
 * 从归一化后的作品名里抽出"第几季 / 第几部分"标记，并返回剥离标记后的基名。
 *
 * 这是消歧的核心：TitleParser 会把季标记粘在标题里
 * （"罗梅莉亚战记 第IIクール" -> "罗梅莉亚战记第IIクール"），
 * 不剥离的话"罗梅莉亚战记"和"罗梅莉亚战记 第2部分"只是字符串包含关系，无法区分。
 *
 * 入参必须是 [TitleNormalizer.normalize] 的结果。
 */
object SeasonMarker {

    data class Parsed(val base: String, val season: Int?)

    /** 带明确关键字的标记，剥离最安全，优先匹配 */
    private val keywordPatterns: List<Regex> = listOf(
        // 第二季 / 第2部分 / 第IIクール / 第3章
        Regex("第([0-9]+|[一二三四五六七八九十]+)(季|期|部分|部|クール|くーる|章|篇|話|话)$"),
        // season2 / part2 / cour2 / 2ndseason
        Regex("(?:season|part|cour)([0-9]+)$"),
        Regex("([0-9]+)(?:st|nd|rd|th)season$"),
        // S2（基名至少保留 2 个字符，避免整名被吃成空）
        Regex("s([0-9]+)$"),
    )

    private const val ROMAN_CHARS = "ivxl"

    fun parse(normalizedTitle: String): Parsed {
        if (normalizedTitle.isEmpty()) {
            return Parsed(normalizedTitle, null)
        }

        for (pattern in keywordPatterns) {
            matchKeyword(pattern, normalizedTitle)?.let { return it }
        }

        matchRoman(normalizedTitle)?.let { return it }
        matchNakedDigit(normalizedTitle)?.let { return it }

        return Parsed(normalizedTitle, null)
    }

    /**
     * 裸数字：罗小黑战记2。只接受单个 2..9 且前一位是汉字，避免把 "86"、"5" 这类纯数字标题切开。
     */
    private fun matchNakedDigit(title: String): Parsed? {
        if (title.length < 2) {
            return null
        }
        val last = title[title.length - 1]
        if (last !in '2'..'9') {
            return null
        }
        if (!isCjk(title[title.length - 2])) {
            return null
        }
        return Parsed(stripTrailingDi(title.substring(0, title.length - 1)), last - '0')
    }

    private fun matchKeyword(pattern: Regex, title: String): Parsed? {
        val match = pattern.find(title) ?: return null
        if (match.range.first <= 0) {
            return null
        }
        val season = toNumber(match.groupValues[1]) ?: return null
        return Parsed(title.substring(0, match.range.first), season)
    }

    /**
     * 尾部罗马数字（NFKC 已经把 Ⅱ 变成 II）。
     * 只接受前面紧邻汉字的情况，避免把英文单词尾部的 i/x 当成季数。
     */
    private fun matchRoman(title: String): Parsed? {
        var numeralEnd = title.length
        val mode = Regex("(クール|くーる|期|季|部)$").find(title)
        if (mode != null) {
            numeralEnd = mode.range.first
        }
        if (numeralEnd <= 1) {
            return null
        }
        var start = numeralEnd
        while (start > 0 && title[start - 1] in ROMAN_CHARS) {
            start--
        }
        if (start == numeralEnd) {
            return null
        }
        val numeral = title.substring(start, numeralEnd)
        if (numeral.isEmpty() || numeral.length > 4 || !isCjk(title[start - 1])) {
            return null
        }
        val season = romanToNumber(numeral) ?: return null
        val base = stripTrailingDi(title.substring(0, start))
        if (base.isEmpty()) {
            return null
        }
        return Parsed(base, season)
    }

    /** "第IIクール"、"罗梅莉亚战记第2" 剥离标记后基名尾部会留下一个 "第"，去掉 */
    private fun stripTrailingDi(base: String): String {
        return if (base.endsWith("第")) base.dropLast(1) else base
    }

    private fun isCjk(ch: Char): Boolean {
        return ch.code in 0x4E00..0x9FFF
    }

    private fun romanToNumber(numeral: String): Int? = when (numeral) {
        "i" -> 1
        "ii" -> 2
        "iii" -> 3
        "iv" -> 4
        "v" -> 5
        "vi" -> 6
        "vii" -> 7
        "viii" -> 8
        "ix" -> 9
        "x" -> 10
        else -> null
    }

    fun toNumber(token: String): Int? {
        if (token.isEmpty()) {
            return null
        }
        token.toIntOrNull()?.let { return it }

        var result = 0
        var section = 0
        for (ch in token) {
            when (ch) {
                '十' -> section = if (section == 0) 10 else section * 10
                '百' -> section = if (section == 0) 100 else section * 100
                else -> {
                    val digit = CN_DIGITS.indexOf(ch)
                    if (digit < 0) {
                        return null
                    }
                    section += digit
                }
            }
        }
        result += section
        return if (result == 0) null else result
    }

    private const val CN_DIGITS = "零一二三四五六七八九"

}
