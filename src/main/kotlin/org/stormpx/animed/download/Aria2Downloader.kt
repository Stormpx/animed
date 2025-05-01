package org.stormpx.animed.download

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.animed.DownloaderConfig
import org.stormpx.animed.aria2.Aria2Client
import java.io.InputStream
import java.net.URI
import kotlin.io.path.Path

class Aria2Downloader(private val option:DownloaderConfig): Downloader {

    companion object{
        private val logger:Logger= LoggerFactory.getLogger(Aria2Downloader.javaClass);


    }
    private val client:Aria2Client= Aria2Client(URI.create(option.uri),option.token)

    private fun processDownloadPath(input:String):String{
        if (input.startsWith("/")){
            return input
        }
        return Path(downloadPath()).resolve(input).normalize().toAbsolutePath().toString()
    }

    override fun downloadUri(uri: String, downloadPath: String): String {
        val result = client.addUri(uri, processDownloadPath(downloadPath))
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
        val result = client.addTorrent(inputStream, processDownloadPath(downloadPath))
        result.error?.let {
            throw RuntimeException("download torrent failed. code: ${it.code} message: ${it.message}")
        }

        if (result.result==null) {
            logger.error("unexpect result occur. $result")
            throw RuntimeException("result is null ");
        }

        return result.result;
    }

    override fun downloadPath(): String {
        val path =  option.downloadPath?:"/download/"
        return if (path.startsWith("/")){
            path
        } else {
            "/$path"
        }
    }


}