package org.stormpx.animed.download

import org.stormpx.animed.DownloaderConfig
import org.stormpx.animed.aria2.Aria2Client
import java.io.InputStream
import java.net.URI
import kotlin.io.path.Path

class Aria2Downloader(private val option:DownloaderConfig): Downloader {

    private val client:Aria2Client= Aria2Client(URI.create(option.uri),option.token)

    private fun processDownloadPath(input:String):String{
        if (input.startsWith("/")){
            return input
        }
        return Path(downloadPath()).resolve(input).normalize().toAbsolutePath().toString()
    }

    override fun downloadUri(uri: String, downloadPath: String): String {
        val targetPath = processDownloadPath(downloadPath)
        val result = client.addUri(uri, targetPath)
        result.error?.let {
            throw IllegalStateException("aria2 refused to download '$uri' into '$targetPath': [${it.code}] ${it.message}")
        }

        return result.result
            ?: throw IllegalStateException("aria2 returned no download id for '$uri' (into '$targetPath'): $result")
    }

    override fun downloadTorrent(inputStream: InputStream, downloadPath: String): String {
        val targetPath = processDownloadPath(downloadPath)
        val result = client.addTorrent(inputStream, targetPath)
        result.error?.let {
            throw IllegalStateException("aria2 refused to download torrent into '$targetPath': [${it.code}] ${it.message}")
        }

        return result.result
            ?: throw IllegalStateException("aria2 returned no download id for torrent (into '$targetPath'): $result")
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