package org.stormpx.animed

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists

class Animed {
    private val fileMap: HashMap<Path,FileLock> = HashMap()
    companion object{
        val animed: Animed = Animed();
    }


    fun markDir(dir:Path){
        if (fileMap.containsKey(dir))
            return;
        try {
            val channel = FileChannel.open(dir.resolve("animed.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)
            val lock = channel.tryLock()
            fileMap[dir] = lock;
        } catch (e: Exception) {
            throw RuntimeException("unbale lock $dir");
        }
    }

    fun isSafe(dir:Path) : Boolean{
        return fileMap.containsKey(dir)&&dir.resolve("animed.lock").exists();
    }


    fun releaseAll(){
        fileMap.forEach { it.value.release() }
    }

}