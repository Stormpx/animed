package org.stormpx.animed

import org.junit.jupiter.api.Test
import org.stormpx.animed.AnimeConfig
import java.nio.file.Path

class WorkerTests {


    @Test
    fun test(){
        val worker = MockWorker(
            Path.of(""),
            AnimeConfig("isekai-ojisan",
                "https://mikanani.me/RSS/Search?searchstr=%E8%99%9A%E6%9E%84%E6%8E%A8%E7%90%86",
                false,
                rules= arrayOf("[ANi] InSpectre S2 - 虚构推理 第二季 - #chapter# [1080P][#mono#][#mono#][AAC AVC][CHT][MP4]#mono#"),
                startChapter = 2.0,
                downloader = "",
                downloadPath = "",
            ),
            MockAnimedContext()
        )


        worker.start()
    }


}