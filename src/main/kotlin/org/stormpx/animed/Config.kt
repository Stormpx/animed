package org.stormpx.animed

import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.modules.EmptySerializersModule
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock

class Config(private val configPath: Path) {
    companion object{
        val yaml = Yaml(
            EmptySerializersModule(), YamlConfiguration(
                encodeDefaults = false,
                strictMode = false,
                breakScalarsAt = 65535,
                sequenceBlockIndent= 2,
                singleLineStringStyle = SingleLineStringStyle.Plain
        )
        )
    }
    private val lock: ReentrantLock = ReentrantLock()

    var latestConfig:AppConfig?=null

    fun readPlain():String{
        return Files.readString(configPath)
    }

    fun readConfig():AppConfig{
        lock.lock()
        try {
            if (!Files.exists(configPath)) {
                throw IllegalStateException("config file '$configPath' does not exist: check the path given on the command line")
            }
            if (!Files.isRegularFile(configPath)) {
                throw IllegalStateException("config file '$configPath' is not a regular file")
            }
            Files.newInputStream(configPath).use{
                return yaml.decodeFromStream(AppConfig.serializer(),it, StandardCharsets.UTF_8)
            }
        }finally {
            lock.unlock()
        }
    }

    fun writeConfig(newAppConfig:AppConfig){
        val configPath = configPath
        lock.lock()
        var tempPath: Path? = null
        try {
            try {
                val temp = Files.createTempFile(configPath.parent,"animed-",".tmp.yaml")
                tempPath = temp
                yaml.encodeToStream(AppConfig.serializer(),newAppConfig,Files.newOutputStream(temp,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.SYNC),StandardCharsets.UTF_8)
                Files.move(temp,configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }catch (e: AccessDeniedException){
                throw IllegalStateException("unable to write config file '$configPath': access denied for '${e.file ?: configPath}'",e)
            }catch (e: IOException){
                throw IllegalStateException("unable to write config file '$configPath': ${e.message ?: e.javaClass.simpleName}",e)
            }finally {
                tempPath?.let { runCatching { Files.deleteIfExists(it) } }
            }
        }finally {
            lock.unlock()
        }
    }

    fun addAnime(anime:AnimeConfig){
        val latest = latestConfig
            ?: throw IllegalStateException("config '$configPath' has not been loaded yet, unable to add anime '${anime.id}'")
        val newAppConfig = latest.copy(anime = latest.anime.filter { config -> config.id != anime.id }.plus(anime).toTypedArray())
        writeConfig(newAppConfig)
    }

    fun removeAnime(id:String){
        val latest = latestConfig
            ?: throw IllegalStateException("config '$configPath' has not been loaded yet, unable to remove anime '$id'")
        if (latest.anime.none { it.id == id }) {
            throw IllegalArgumentException("anime '$id' is not configured, nothing to remove")
        }
        val newAppConfig = latest.copy(anime = latest.anime.filter { config -> config.id != id }.toTypedArray())
        writeConfig(newAppConfig)
    }

}