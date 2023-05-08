package org.stormpx.animed

import org.stormpx.animed.download.Downloader
import java.io.InputStream

class MockDownloader : Downloader {
    override fun downloadUri(uri: String, downloadPath: String): String {
        return ""
    }

    override fun downloadTorrent(inputStream: InputStream, downloadPath: String): String {
        return ""
    }
}