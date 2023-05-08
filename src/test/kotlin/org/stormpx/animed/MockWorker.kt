package org.stormpx.animed

import org.stormpx.animed.AnimeConfig
import org.stormpx.animed.AnimedContext
import org.stormpx.animed.Worker
import java.nio.file.Path

class MockWorker(dataPath: Path, animeConfig: AnimeConfig, context: AnimedContext) :
    Worker(dataPath, animeConfig, context) {


    override fun getAnimeData(): AnimeData {
        return AnimeData("mock",11.0,ArrayList());
    }

    override fun saveAnimeData(anime: AnimeData) {
    }
}