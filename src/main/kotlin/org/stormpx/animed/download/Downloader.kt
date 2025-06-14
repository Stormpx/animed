package org.stormpx.animed.download

import java.io.InputStream

interface Downloader {

    fun downloadUri(uri:String,downloadPath:String):String

    fun downloadTorrent(inputStream: InputStream,downloadPath:String): String

    fun downloadPath():String

}