package org.stormpx.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TitleParserTests {

    private final static List<TitleTestCase> testcases=List.of(
            new TitleTestCase("[NC-Raws] 秋叶原女仆战争 / Akiba Maid Sensou - 12 (B-Global 1920x1080 HEVC AAC MKV)",
                    AnimeTitleBuilder.builder().subGroup("NC-Raws").addAnimeTitle("秋叶原女仆战争").addAnimeTitle("AkibaMaidSensou")
                            .videoResolution("1920x1080").videoSourceName("B-Global").episode(12.0).build()),
            new TitleTestCase("[Lilith-Raws] 殺手奶爸 / Buddy Daddies - 12 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4]",
                    AnimeTitleBuilder.builder().subGroup("Lilith-Raws").addAnimeTitle("殺手奶爸").addAnimeTitle("BuddyDaddies")
                            .videoResolution("1080p").videoSourceName("Baha").videoSourceType("WEB-DL").language("CHT").episode(12.0).build()),
            new TitleTestCase("[LoliHouse] 杀手奶爸 / Buddy Daddies - 09 [WebRip 1080p HEVC-10bit AAC][简繁内封字幕]",
                    AnimeTitleBuilder.builder().subGroup("LoliHouse").addAnimeTitle("杀手奶爸").addAnimeTitle("BuddyDaddies")
                            .videoResolution("1080p").videoSourceType("WebRip").language("简繁内封字幕").episode(9.0).build()),
            new TitleTestCase("[LoliHouse] 文豪野犬 第四季 / Bungou Stray Dogs Season 4 - 13(50) [WebRip 1080p HEVC-10bit AAC][简繁内封字幕][END]",
                    AnimeTitleBuilder.builder().subGroup("LoliHouse").addAnimeTitle("文豪野犬第四季").addAnimeTitle("BungouStrayDogsSeason4")
                            .videoResolution("1080p").videoSourceType("WebRip").language("简繁内封字幕").episode(13.0).build()),
            new TitleTestCase("【幻樱字幕组】【10月新番】【电锯人 Chainsaw Man】【05】【BIG5_MP4】【1920X1080】",
                    AnimeTitleBuilder.builder().subGroup("幻樱字幕组").addAnimeTitle("电锯人ChainsawMan").episode(5.0).videoResolution("1920X1080").language("BIG5").build()),
            new TitleTestCase("[LoliHouse] 文豪野犬 第四季 / Bungou Stray Dogs Season 4 - 13(50) [WebRip 1080p HEVC-10bit AAC][简繁内封字幕][END]",
                    AnimeTitleBuilder.builder().subGroup("LoliHouse").addAnimeTitle("文豪野犬第四季").addAnimeTitle("BungouStrayDogsSeason4").episode(13.0).videoSourceType("WebRip").videoResolution("1080p").language("简繁内封字幕").build()),
            new TitleTestCase("[漫猫字幕社][10月新番][灵能百分百][Mob Psycho 100 III][12Fin][1080p][MP4][GB][简中]",
                    AnimeTitleBuilder.builder().subGroup("漫猫字幕社").addAnimeTitle("灵能百分百").addAnimeTitle("MobPsycho100III").episode(12.0).videoResolution("1080p").language("简中").build()),
            new TitleTestCase("[GalaxyRailroad-888] 游戏王GO RUSH!! Yu-Gi-Oh! GO RUSH !! 056 720P [GB_简中]",
                    AnimeTitleBuilder.builder().subGroup("GalaxyRailroad-888").addAnimeTitle("游戏王GORUSH!!Yu-Gi-Oh!GORUSH!!").episode(56.0).videoResolution("720P").language("简中").build()),
            new TitleTestCase("[Skymoon-Raws] 【我推的孩子】 / Oshi no Ko - 04 [ViuTV][WEB-RIP][1080p][AVC AAC][CHT][SRT][MKV](先行版本)",
                    AnimeTitleBuilder.builder().subGroup("Skymoon-Raws").addAnimeTitle("我推的孩子").addAnimeTitle("OshinoKo").episode(4.0).videoSourceName("ViuTV").videoSourceType("WEB-RIP").videoResolution("1080p").language("CHT").build()),
            new TitleTestCase("[北宇治字幕组&霜庭云花Sub&氢气烤肉架]【我推的孩子】/【Oshi no ko】[03 V2][Webrip][1080p][HEVC_AAC][简繁日内封]",
                    AnimeTitleBuilder.builder().subGroup("北宇治字幕组&霜庭云花Sub&氢气烤肉架").animeTitle(List.of("我推的孩子","Oshinoko")).episode(3.0).videoSourceType("Webrip").videoResolution("1080p").language("简繁日内封").build()),
            new TitleTestCase("[豌豆字幕组&风之圣殿字幕组&LoliHouse] 鬼灭之刃 刀匠村篇 04(48) / Kimetsu no Yaiba - 48 [WebRip 1080p HEVC-10bit AAC][简繁外挂字幕]",
                    AnimeTitleBuilder.builder().subGroup("豌豆字幕组&风之圣殿字幕组&LoliHouse").animeTitle(List.of("鬼灭之刃刀匠村篇04","KimetsunoYaiba")).episode(48.0).videoSourceType("WebRip").videoResolution("1080p").language("简繁外挂字幕").build()),

            new TitleTestCase("[漫游字幕组] Mobile Suit Gundam The Witch from Mercury 机动战士高达 水星的魔女 第16话 Webrip 1080p MKV 简繁外挂",
                    AnimeTitleBuilder.builder().subGroup("漫游字幕组").addAnimeTitle("MobileSuitGundamTheWitchfromMercury机动战士高达水星的魔女").episode(16.0).videoResolution("1080p").videoSourceType("Webrip").language("简繁外挂").build()),

            new TitleTestCase("[成子坂地下室]爱丽丝机甲 Expansion(机战少女Alice) /Alice Gear Aegis Expansion - 05 [简中内嵌][1080p][AVC AAC][MP4][附ASS]",
                    AnimeTitleBuilder.builder().subGroup("成子坂地下室").addAnimeTitle("爱丽丝机甲Expansion").addAnimeTitle("机战少女Alice").addAnimeTitle("/AliceGearAegisExpansion").episode(5.0).videoResolution("1080p").language("简中内嵌").build()),
            new TitleTestCase("[丸子家族][海螺小姐(Sazae-san)][2694-2698][2023.04][简日_繁日内封][1080P][hevc-10bit_aac][MKV]",
                    AnimeTitleBuilder.builder().subGroup("丸子家族").animeTitle(List.of("海螺小姐","Sazae-san")).episode(2694.0).videoResolution("1080P").language("繁日内封").build()),
            new TitleTestCase("[MingY] 变成狗后被喜欢的人捡了。 / Inu ni Nattara Suki na Hito ni Hirowareta. [OVA02][完全wonderful版][[1080p]简日内嵌]（招募）",
                    AnimeTitleBuilder.builder().subGroup("MingY").animeTitle(List.of("变成狗后被喜欢的人捡了。","InuniNattaraSukinaHitoniHirowareta.")).episode(2.0).videoResolution("1080p").language("简日内嵌").build()),
            new TitleTestCase("[夜莺家族][樱桃小丸子第二期(Chibi Maruko-chan II)][1384]姐姐结交了秀友&小丸子享受香味[2023.04.30][GB_JP][1080P][MP4]",
                    AnimeTitleBuilder.builder().subGroup("夜莺家族").animeTitle(List.of("樱桃小丸子第二期","ChibiMaruko-chanII")).episode(1384.0).videoResolution("1080P").language("JP").build()),
            new TitleTestCase("[DAY字幕组][赛马娘 Pretty Derby 直道登顶 / Uma Musume: Pretty Derby Road to the Top][第3话 奔跑的理由][简体中文][AMZN.WEBrip][1080P][MP4]（含赛事注释）",
                    AnimeTitleBuilder.builder().subGroup("DAY字幕组").animeTitle(List.of("赛马娘PrettyDerby直道登顶","UmaMusume:PrettyDerbyRoadtotheTop")).episode(3.0).videoResolution("1080P").videoSourceName("AMZN").videoSourceType("WEBrip").language("简体中文").build()),

            new TitleTestCase("[爱恋字幕社&漫猫字幕社] 水星领航员/ARIA The ANIMATION (01-13Fin BDRIP 1080p AVC AAC MP4 2005年10月 简中)",
                    AnimeTitleBuilder.builder().subGroup("爱恋字幕社&漫猫字幕社").animeTitle(List.of("水星领航员/ARIATheANIMATION")).episode(1.0).videoResolution("1080p").videoSourceType("BDRIP").language("简中").build())
    );

    record TitleTestCase(String title,AnimeTitle expectedTitle){

    }

    private void assertTitleEq(AnimeTitle expected,AnimeTitle actual){
        assertEquals(expected.subGroup(),actual.subGroup(), expected +"\n"+ actual);
        if (expected.animeTitle()!=null&&actual.animeTitle()!=null){
            assertEquals(Set.of(expected.animeTitle()),Set.of(actual.animeTitle()), expected +"\n"+ actual);
        }
        assertEquals(expected.videoResolution(),actual.videoResolution(), expected +"\n"+ actual);
        assertEquals(expected.videoSourceName(),actual.videoSourceName(), expected +"\n"+ actual);
        assertEquals(expected.videoSourceType(),actual.videoSourceType(), expected +"\n"+ actual);
        assertEquals(expected.episode(),actual.episode(), expected +"\n"+ actual);
        assertEquals(expected.language(),actual.language(), expected +"\n"+ actual);

    }

    @Test
    public void test(){
        for (TitleTestCase testcase : testcases) {
            System.out.println(testcase.title);
            AnimeTitle title = new TitleParser().parse(testcase.title);
            assertTitleEq(testcase.expectedTitle,title);

        }
    }

}
