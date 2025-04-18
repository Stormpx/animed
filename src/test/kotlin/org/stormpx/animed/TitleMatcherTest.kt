package org.stormpx.animed

import org.junit.jupiter.api.Test

class TitleMatcherTest{

    data class TestCase(val base: String, val results:Array<TestResult> )

    data class TestResult(val title:String, val match: Boolean, val chapter: Double)

    @Test
    fun testMatch(){
        val testcases = arrayOf(
            TestCase(
                "[dwadwa dwadw] Bucchigire! - 15.5 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]",
                arrayOf(
                    TestResult("[dwadwa dwadw] Bucchigire! - 15.5 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]", true, 15.5),
                    TestResult("[dwadwa dwadw] Bucchigire! - 08 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]", true, 8.0),
                    TestResult("[dwadwa dwadw] Bucchigire! - 1 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]", true, 1.0),
                    TestResult("[different here] Bucchigire! - 07 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]", false, -1.0),
                )
            ),
            TestCase("[酷漫404][夏日重现][12][1080P][WebRip][简日双语][AVC AAC][MP4][字幕组招人内详]",
                arrayOf(
                    TestResult("[酷漫404][夏日重现][12][1080P][WebRip][简日双语][AVC AAC][MP4][字幕组招人内详]" ,true,12.0),
                    TestResult("[酷漫404][夏日重现][11][1080P][WebRip][简日双语][AVC AAC][MP4][字幕组招人内详]" ,true,11.0),
                    TestResult("[酷漫404][夏日重现][01][1080P][WebRip][简日双语][AVC AAC][MP4][招人]" ,true,1.0),
                )
            ),
            TestCase("【幻月压制】【22年日剧】【欢迎光临自助洗衣店】【04】【1080P】【中文字幕】",
                arrayOf(
                    TestResult("【幻月压制】【22年日剧】【欢迎光临自助洗衣店】【04】【1080P】【中文字幕】" ,true,4.0),
                    TestResult("【幻月压制】【日剧】【欢迎光临自助洗衣店】【04】【1080P】【中文】" ,false,-1.0),
                )
            ),
            TestCase("【喵萌奶茶屋】★07月新番★[异世界舅舅/Isekai Ojisan][03][1080p][简日双语][招募翻译]" ,
                arrayOf(
                    TestResult("【喵萌奶茶屋】★07月新番★[异世界舅舅/Isekai Ojisan][03][1080p][简日双语][招募翻译]" ,true,3.0),
                    TestResult("【喵萌奶茶屋】【日剧】【欢迎光临自助洗衣店】【04】【1080P】【中文】" ,false,-1.0),
                )
            ),
            TestCase("[织梦字幕组]Summer Time Rendering 夏日重现[14][2022.07.15][1080P][GB_JP][AVC]",
                arrayOf(
                    TestResult("[织梦字幕组]Summer Time Rendering 夏日重现[14][2022.07.15][1080P][GB_JP][AVC]",true,14.0),
                    TestResult("[织梦字幕组]Summer Time Rendering 夏日重现[13][2022.07.08][1080P][GB_JP][AVC]",true,13.0),
                    TestResult("[织梦字幕组]Summer Time Rendering 夏日重现[13][2022.07.08][720P][JP][AVCC]",false,-1.0),
                )
            ),
            TestCase("【恭喜复播】【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][08][1080p][简日双语][招募翻译] [576.3MB]",
                arrayOf(
                    TestResult("【恭喜复播】【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][11][1080p][简日双语][招募翻译] [576.3MB]",true,11.0),
                    TestResult("【恭喜复播第二轮】【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][16][1080p][简日双语][招募翻译] [576.3MB]",true,16.0),
                    TestResult("【喵萌奶茶屋】★10月新番★[异世界舅舅/Isekai Ojisan][11][1080p][简日双语][招募翻译]",true,11.0)
                )
            ),
            TestCase("[ANi] InSpectre S2 - 虚构推理 第二季 - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
                arrayOf(
                    TestResult("[ANi] InSpectre S2 - 虚构推理 第二季 - 04 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",true,4.0),
                    TestResult("[ANi] InSpectre S2 -  虚构推理 第二季 - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",true,3.0),
                )
            ),
            TestCase("機動戰士Gundam GQuuuuuuX S01E02 1080p 日英雙語-多國字幕",
                arrayOf(
                    TestResult("機動戰士Gundam GQuuuuuuX S01E02 1080p 日英雙語-多國字幕",true,2.0),
                    TestResult("機動戰士Gundam GQuuuuuuX S01e10 1080p 日英雙語-多國字幕",true,10.0),
                    TestResult("機動戰士Gundam GQuuuuuuX s02e01 1080p 日英雙語-多國字幕",true,1.0),
                )
            ),

            TestCase("[不当舔狗制作组] 机动战士高达 GQuuuuuuX - 01 [AMZN WebRip 暴力1080p HEVC-10bit E-AC-3][简繁内封字幕]",
                arrayOf(
                    TestResult("[不当舔狗制作组] 机动战士高达 GQuuuuuuX - 01 [AMZN WebRip 暴力1080p HEVC-10bit E-AC-3][简繁内封字幕]",true,1.0),
                    TestResult("[不当舔狗制作组] 机动战士高达 GQuuuuuuX - 01 [AMZN WebRip 1080p HEVC-10bit E-AC-3][简繁内封字幕]",true,1.0),
                )
            ),
        )

        testcases.forEach {
            val matcher = TitleMatcher(it.base)
            it.results.forEach { r ->
                val result =matcher.match(r.title)
                println(r.title)
                kotlin.test.assertEquals(r.match, result.match)
                if (r.match)
                    kotlin.test.assertEquals(r.chapter, result.chapter)
            }

        }



    }

}