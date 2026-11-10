package org.stormpx.animed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.rss.Channel
import org.stormpx.rss.RSSReader
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import kotlin.io.path.exists

class Animed {
    private val fileMap: HashMap<Path,FileLock> = HashMap()
    companion object{
        private val logger: Logger = LoggerFactory.getLogger(Animed::class.java)
        val INSTANCE: Animed = Animed();
    }


    fun markDir(dir:Path){
        if (fileMap.containsKey(dir))
            return;
        val lockFile = dir.resolve("animed.lock")
        try {
            val channel = FileChannel.open(lockFile,StandardOpenOption.CREATE,StandardOpenOption.WRITE)
            val lock = channel.tryLock()
            if (lock == null) {
                channel.close()
                throw IllegalStateException("data directory '$dir' is already in use: lock file '$lockFile' is held by another animed process")
            }
            fileMap[dir] = lock;
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                "unable to lock data directory '$dir' with lock file '$lockFile': ${e.message ?: e.javaClass.simpleName}. " +
                    "Make sure the directory exists and is writable", e
            )
        }
    }

    fun isSafe(dir:Path) : Boolean{
        return fileMap.containsKey(dir)&&dir.resolve("animed.lock").exists();
    }


    fun releaseAll(){
        fileMap.forEach { it.value.release() }
    }


    fun getChannel(url:String): Channel{
        val response =
            Http.client.send(
                HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofMinutes(1)).build(),
                BodyHandlers.ofInputStream()
            )
        logger.info("request $url status_code = ${response.statusCode()}")
        if (response.statusCode() != 200) {
            throw RuntimeException("HTTP ${response.statusCode()}")
        }
        return RSSReader(response.body()).read()
    }

    fun newMatchers(patterns: Array<String>?, titles: Array<String>?): Array<Matcher>{
        val title:Array<Matcher> = titles?.map { TitleMatcher(it) }?.toTypedArray()?: emptyArray()
        val rules:Array<Matcher> = patterns?.map { EpisodeMatcher(it) }?.toTypedArray()?: emptyArray()
        return title+rules
    }

}