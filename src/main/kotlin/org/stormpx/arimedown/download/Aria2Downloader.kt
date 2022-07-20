package org.stormpx.arimedown.download

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.arimedown.DownloaderOption
import org.stormpx.arimedown.aria2.Aria2Client
import java.io.InputStream
import java.net.URI

class Aria2Downloader(option:DownloaderOption): Downloader {

    companion object{
        private val logger:Logger= LoggerFactory.getLogger(Aria2Downloader.javaClass);


    }
    private val client:Aria2Client= Aria2Client(URI.create(option.uri),option.token)


    override fun downloadUri(uri: String, downloadPath: String): String {
        val result = client.addUri(uri, downloadPath)
        result.error?.let {
            throw RuntimeException("download $uri failed. code: ${it.code} message: ${it.message}")
        }

        if (result.result==null) {
            logger.error("unexpect result occur. $result")
            throw RuntimeException("result is null ");
        }

        return result.result;
    }

    override fun downloadTorrent(inputStream: InputStream, downloadPath: String): String {
        val result = client.addTorrent(inputStream, downloadPath)
        result.error?.let {
            throw RuntimeException("download torrent failed. code: ${it.code} message: ${it.message}")
        }

        if (result.result==null) {
            logger.error("unexpect result occur. $result")
            throw RuntimeException("result is null ");
        }

        return result.result;
    }


}