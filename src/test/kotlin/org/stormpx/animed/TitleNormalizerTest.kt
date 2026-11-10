package org.stormpx.animed

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitleNormalizerTest {

    @Test
    fun testTraditionalToSimplified() {
        assertEquals("罗梅莉亚战记", TitleNormalizer.normalize("羅梅莉亞戰記"))
        assertEquals("间谍过家家", TitleNormalizer.normalize("間諜過家家"))
        assertEquals("咒术回战", TitleNormalizer.normalize("咒術迴戰"))
        // 短语级消歧：頭髮 -> 头发，单字表做不到
        assertEquals("头发", TitleNormalizer.normalize("頭髮"))
    }

    @Test
    fun testJapaneseVariant() {
        // 片假名会被折叠成平假名：ロメリア -> ろめりあ
        assertEquals("ろめりあ战记", TitleNormalizer.normalize("ロメリア戦記"))
        // 片假名会被折叠成平假名，所以这里的バスケ -> ばすけ
        assertEquals("黑子のばすけ", TitleNormalizer.normalize("黒子のバスケ"))
        assertEquals("图书馆战争", TitleNormalizer.normalize("図書館戦争"))
    }

    @Test
    fun testKatakanaFoldedToHiragana() {
        assertEquals(
            TitleNormalizer.normalize("ロメリア戦記"),
            TitleNormalizer.normalize("ろめりあ戦記")
        )
    }

    @Test
    fun testPunctuationAndSpaceRemoved() {
        assertEquals("romeliasenki", TitleNormalizer.normalize("Romelia Senki"))
        assertEquals("romeliasenki", TitleNormalizer.normalize("RomeliaSenki"))
        assertEquals("罗梅莉亚战记", TitleNormalizer.normalize("罗梅莉亚战记！"))
        assertEquals("罗梅莉亚战记", TitleNormalizer.normalize(" 罗梅莉亚战记 "))
        assertEquals("spyfamily", TitleNormalizer.normalize("SPY×FAMILY"))
        // 全角字母折成半角
        assertEquals("romelia", TitleNormalizer.normalize("ＲＯＭＥＬＩＡ"))
        assertEquals("", TitleNormalizer.normalize(null))
        assertEquals("", TitleNormalizer.normalize("   "))
    }

    @Test
    fun testSeasonMarkerStaysInNormalizedText() {
        // 归一化只负责字形，不负责剥季标记，季标记由 SeasonMarker 处理
        assertEquals("罗梅莉亚战记第2部分", TitleNormalizer.normalize("罗梅莉亚战记 第2部分"))
        assertEquals("ろめりあ战记第iiくーる", TitleNormalizer.normalize("ロメリア戦記 第IIクール"))
    }

    @Test
    fun testSimilarity() {
        assertEquals(1.0, TitleNormalizer.similarity("罗梅莉亚战记", "罗梅莉亚战记"))
        assertTrue(TitleNormalizer.similarity("罗梅莉亚战记", "罗小黑战记") < 0.6)
        // 带季后缀时整名相似度会被长度摊薄，季标记的区分交给 SeasonMarker + 前缀规则
        assertTrue(TitleNormalizer.similarity("罗梅莉亚战记", "罗梅莉亚战记第2部分") > 0.5)
    }

}
