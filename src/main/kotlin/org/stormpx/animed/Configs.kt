package org.stormpx.animed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class AppConfig(
    val debug: Boolean =false,
    @SerialName("data_path")
    val dataPath:String,
    @SerialName("default_target")
    val defaultTarget:String? = null,
    val anime:Array<AnimeConfig>,
    val downloader: Array<DownloaderConfig>,
    val email:EmailConfig? = null,
    val users: Array<UserConfig>? = null,
    val proxies: Array<String>?=null,
    val mcp: McpConfig?=null,
    ){

    fun path(): Path {
        return Path(dataPath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppConfig

        if (debug != other.debug) return false
        if (dataPath != other.dataPath) return false
        if (defaultTarget != other.defaultTarget) return false
        if (!anime.contentEquals(other.anime)) return false
        if (!downloader.contentEquals(other.downloader)) return false
        if (email != other.email) return false
        if (users != null) {
            if (other.users == null) return false
            if (!users.contentEquals(other.users)) return false
        } else if (other.users != null) return false
        if (proxies != null) {
            if (other.proxies == null) return false
            if (!proxies.contentEquals(other.proxies)) return false
        } else if (other.proxies != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = debug.hashCode()
        result = 31 * result + dataPath.hashCode()
        result = 31 * result + defaultTarget.hashCode()
        result = 31 * result + anime.contentHashCode()
        result = 31 * result + downloader.contentHashCode()
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (users?.contentHashCode() ?: 0)
        result = 31 * result + (proxies?.contentHashCode() ?: 0)
        return result
    }

}

private val idRegex: Regex= Regex.escapeReplacement("#id#").toRegex()

@Serializable
data class AnimeConfig(
    val id:String,
    val rss:String,
    val immediately: Boolean = false,
    @SerialName("start_episode")
    val startEpisode: Double = -1.0,
    @SerialName("final_episode")
    val finalEpisode: Double? = null,
    @SerialName("refresh_interval")
    val refreshInterval: Long =3600,
    val patterns:Array<String>?=null,
    val titles:Array<String>?=null,
    val downloader: String,
    @SerialName("download_path")
    val downloadPath: String,
    val targets: Array<String>?=null,
    ){

    fun downloadPath(): String{
        return downloadPath.replace(idRegex,id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimeConfig

        if (id != other.id) return false
        if (rss != other.rss) return false
        if (immediately != other.immediately) return false
        if (startEpisode != other.startEpisode) return false
        if (refreshInterval != other.refreshInterval) return false
        if (patterns != null) {
            if (other.patterns == null) return false
            if (!patterns.contentEquals(other.patterns)) return false
        } else if (other.patterns != null) return false
        if (titles != null) {
            if (other.titles == null) return false
            if (!titles.contentEquals(other.titles)) return false
        } else if (other.titles != null) return false
        if (downloader != other.downloader) return false
        if (downloadPath != other.downloadPath) return false
        if (targets != null) {
            if (other.targets == null) return false
            if (!targets.contentEquals(other.targets)) return false
        } else if (other.targets != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rss.hashCode()
        result = 31 * result + immediately.hashCode()
        result = 31 * result + startEpisode.hashCode()
        result = 31 * result + refreshInterval.hashCode()
        result = 31 * result + (patterns?.contentHashCode() ?: 0)
        result = 31 * result + (titles?.contentHashCode() ?: 0)
        result = 31 * result + downloader.hashCode()
        result = 31 * result + downloadPath.hashCode()
        result = 31 * result + (targets?.contentHashCode() ?: 0)
        return result
    }

}

@Serializable
data class DownloaderConfig(
    val id:String,
    val uri:String,
    val token:String?=null,
    @SerialName("download_path")
    val downloadPath: String?=null
)
@Serializable
data class EmailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password:String
)

@Serializable
data class UserConfig(
    val name:String,
    val email:String
)

@Serializable
data class McpConfig(
    val enable: Boolean?=false,
    val host: String?=null,
    val port: Int?=null,
)