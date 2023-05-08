package org.stormpx.animed

import org.stormpx.animed.AnimedContext
import org.stormpx.animed.download.Downloader
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class MockAnimedContext : AnimedContext {
    val threadPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    override fun getDownloader(id: String): Downloader? {
        return MockDownloader()
    }

    override fun scheduler(): ScheduledExecutorService {
        return threadPool;
    }

    override fun notice(message: AnimedContext.AnimeMessage) {
    }
}