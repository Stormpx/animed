package org.stormpx.animed

import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.modules.EmptySerializersModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.concurrent.locks.ReentrantLock

class Config(private val configPath: Path) {
    companion object{
        private val logger: Logger = LoggerFactory.getLogger(Config::class.java)
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
            Files.newInputStream(configPath).use{
                return yaml.decodeFromStream(AppConfig.serializer(),it, StandardCharsets.UTF_8)
            }
        }finally {
            lock.unlock()
        }
    }

    fun writeConfig(newAppConfig:AppConfig){
        val configPath = configPath
        val tempPath = Files.createTempFile(configPath.parent,"animed-",".tmp.yaml")
        yaml.encodeToStream(AppConfig.serializer(),newAppConfig,Files.newOutputStream(tempPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.SYNC),StandardCharsets.UTF_8)

        lock.lock()
        try {
            Files.move(tempPath,configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }catch (e: AccessDeniedException){
            logger.error("",e)
            throw RuntimeException("access denied")
        }finally {
            lock.unlock()
            Files.deleteIfExists(tempPath)
        }
    }

    fun addAnime(anime:AnimeConfig){
        latestConfig?.let {
            val newAppConfig = it.copy(anime = it.anime.filter { config -> config.id != anime.id }.plus(anime).toTypedArray())
            try {
                writeConfig(newAppConfig)
            }catch (e:AccessDeniedException){
                logger.error("",e)
                throw RuntimeException("access denied")
            }
        }
    }

    fun removeAnime(id:String){
        latestConfig?.let {
            val newAppConfig = it.copy(anime = it.anime.filter { config -> config.id != id }.toTypedArray())
            try {
                writeConfig(newAppConfig)
            }catch (e:AccessDeniedException){
                logger.error("",e)
                throw RuntimeException("access denied")
            }
        }
    }

}