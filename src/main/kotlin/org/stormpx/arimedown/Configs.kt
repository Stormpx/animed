package org.stormpx.arimedown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.Path

@Serializable
data class AppConfig(
    @SerialName("data_path")
    val dataPath:String,
    val anime:Array<AnimeConfig>,
    val downloader: Array<DownloaderConfig>){

    fun path(): Path {
        return Path(dataPath)
    }

}

@Serializable
data class AnimeConfig(
    val id:String,
    val rss:String,
    val immediately: Boolean = false,
    @SerialName("start_chapter")
    val startChapter: Double = -1.0,
    @SerialName("refresh_interval")
    val refreshInterval: Long =3600,
    val rules:Array<String>,
    val downloader: String,
    @SerialName("download_path")
    val downloadPath: String
    ){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimeConfig

        if (id != other.id) return false
        if (rss != other.rss) return false
        if (immediately != other.immediately) return false
        if (startChapter != other.startChapter) return false
        if (refreshInterval != other.refreshInterval) return false
        if (!rules.contentEquals(other.rules)) return false
        if (downloader != other.downloader) return false
        if (downloadPath != other.downloadPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rss.hashCode()
        result = 31 * result + immediately.hashCode()
        result = 31 * result + startChapter.hashCode()
        result = 31 * result + refreshInterval.hashCode()
        result = 31 * result + rules.contentHashCode()
        result = 31 * result + downloader.hashCode()
        result = 31 * result + downloadPath.hashCode()
        return result
    }
}

@Serializable
data class DownloaderConfig(
    val id:String,
    val uri:String,
    val token:String?=null
)

