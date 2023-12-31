package org.stormpx.animed

import org.stormpx.animed.download.Downloader
import java.util.concurrent.ScheduledExecutorService

interface AnimedContext {

    data class AnimeMessage(val users:Array<String>, val from:String, val subject:String, val content:String)

    fun getDownloader(id:String) :  Downloader?


    fun scheduler(): ScheduledExecutorService


    fun notice(message:AnimeMessage)


}