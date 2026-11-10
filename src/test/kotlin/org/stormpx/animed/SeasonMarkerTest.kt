package org.stormpx.animed

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeasonMarkerTest {

    private fun parse(raw: String): SeasonMarker.Parsed {
        return SeasonMarker.parse(TitleNormalizer.normalize(raw))
    }

    @Test
    fun testChineseSeasonMarker() {
        assertEquals(SeasonMarker.Parsed("罗小黑战记", 2), parse("罗小黑战记 第二季"))
        assertEquals(SeasonMarker.Parsed("罗梅莉亚战记", 2), parse("罗梅莉亚战记 第2部分"))
        assertEquals(SeasonMarker.Parsed("虚构推理", 2), parse("虚构推理 第二季"))
        assertEquals(SeasonMarker.Parsed("进击的巨人", 3), parse("进击的巨人 第三季"))
        assertEquals(SeasonMarker.Parsed("某科学的超电磁炮", 3), parse("某科学的超电磁炮 第3期"))
    }

    @Test
    fun testJapaneseCourMarker() {
        assertEquals(SeasonMarker.Parsed("ろめりあ战记", 2), parse("ロメリア戦記 第IIクール"))
        assertEquals(SeasonMarker.Parsed("ろめりあ战记", 2), parse("ロメリア戦記 第2クール"))
        // 没有"第"的裸罗马数字：記Ⅲ
        assertEquals(SeasonMarker.Parsed("ろめりあ战记", 3), parse("ロメリア戦記Ⅲ"))
    }

    @Test
    fun testLatinSeasonMarker() {
        assertEquals(SeasonMarker.Parsed("inspectre", 2), parse("InSpectre S2"))
        assertEquals(SeasonMarker.Parsed("进击的巨人", 2), parse("进击的巨人 Season 2"))
        assertEquals(SeasonMarker.Parsed("进击的巨人", 2), parse("进击的巨人 2nd Season"))
        assertEquals(SeasonMarker.Parsed("进击的巨人", 2), parse("进击的巨人 part2"))
    }

    @Test
    fun testNakedTrailingDigit() {
        assertEquals(SeasonMarker.Parsed("罗小黑战记", 2), parse("罗小黑战记2"))
        // 纯数字标题不能被切开
        assertEquals(SeasonMarker.Parsed("86", null), parse("86"))
        // 数字前面不是汉字时不当成季标记
        assertEquals(SeasonMarker.Parsed("rezero2", null), parse("Re:Zero 2"))
    }

    @Test
    fun testNoMarker() {
        assertEquals(SeasonMarker.Parsed("罗梅莉亚战记", null), parse("罗梅莉亚战记"))
        assertEquals(SeasonMarker.Parsed("间谍过家家", null), parse("間諜過家家"))
        // 篇章名不是季标记，保留在基名里
        assertEquals(SeasonMarker.Parsed("罗小黑战记众生之门篇", null), parse("罗小黑战记 众生之门篇"))
        assertNull(parse("罗梅莉亚战记").season)
    }

    @Test
    fun testChineseNumerals() {
        assertEquals(1, SeasonMarker.toNumber("一"))
        assertEquals(10, SeasonMarker.toNumber("十"))
        assertEquals(12, SeasonMarker.toNumber("十二"))
        assertEquals(20, SeasonMarker.toNumber("二十"))
        assertEquals(21, SeasonMarker.toNumber("二十一"))
        assertNull(SeasonMarker.toNumber(""))
    }

}
