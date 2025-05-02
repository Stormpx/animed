package org.stormpx.animed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.rss.Channel
import org.stormpx.rss.RSSReader
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.time.Duration

class AnimeRss {
    enum class Website(val url: String) {
        DMHY("https://dmhy.org/topics/rss/rss.xml"),
        MIKAN("https://mikanani.me/RSS/Search"),
        NYAASI("https://nyaa.si/?page=rss&c=0_0&f=0"),
        BANGUMI("https://bangumi.moe/rss/search"),
        
    }
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AnimeRss::class.java)
    }


    private fun getUrl(website: Website, keyword: String):String{
        return when(website){
            Website.DMHY -> "${website.url}?keyword=${URLEncoder.encode(keyword,StandardCharsets.UTF_8)}"
            Website.MIKAN -> "${website.url}?searchstr=${URLEncoder.encode(keyword,StandardCharsets.UTF_8)}"
            Website.NYAASI -> "${website.url}&q=${URLEncoder.encode(keyword,StandardCharsets.UTF_8)}"
            Website.BANGUMI -> "${website.url}/${URLEncoder.encode(keyword,StandardCharsets.UTF_8)}"
        }
    }

    fun getRssContent(website: Website,keyword: String):RssContent{
        val url = getUrl(website, keyword)
        val response =
            Http.client.send(
                HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofMinutes(1)).build(),
                BodyHandlers.ofInputStream()
            )
        logger.info("request $url status_code = ${response.statusCode()}")
        if (response.statusCode() != 200) {
            throw RuntimeException("")
        }
        val channel = RSSReader(response.body()).read()
        return RssContent(url,channel)
    }

}

data class RssContent(val url:String, val channel:Channel)