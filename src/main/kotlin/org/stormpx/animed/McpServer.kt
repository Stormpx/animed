package org.stormpx.animed

import DieOtaku
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.*
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.util.collections.ConcurrentMap
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.ServerSession
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter

class McpServer(private val otaku: DieOtaku) {
    companion object{
        private val logger: Logger = LoggerFactory.getLogger(McpServer::class.java)
    }
    @Serializable
    data class AnimeItem(val id:String,val title:String,val pubTime: String)
    @Serializable
    data class ListAnime(val url:String, val animes:List<AnimeItem>)

    private var server:EmbeddedServer<CIOApplicationEngine,CIOApplicationEngine.Configuration>?=null
    var host: String?=null
    var port: Int?=null


    private fun configureServer(): Server {
        val server = Server(
            Implementation(
                name = "Animed mcp server",
                version = "0.0.1"
            ),
            ServerOptions(
                capabilities = ServerCapabilities(
                    resources = ServerCapabilities.Resources(subscribe = true, listChanged = true),
                    tools = ServerCapabilities.Tools(listChanged = true)
                )
            )
        )



        server.addTool(
            name = "get-animed-config",
            description = "获取Animed服务器的配置文件,没有参数,返回的内容是yaml格式".trimIndent(),
            inputSchema = ToolSchema()
        ) { request ->
            CallToolResult(
                content = listOf(TextContent(otaku.config.readPlain()))
            )
        }


        server.addTool(
            name = "animed-list",
            description = "使用提供的关键词去指定的动漫资源网站搜索可下载番剧".trimIndent(),
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("source") {
                        put("type", "string")
                    }
                    putJsonObject("keyword") {
                        put("type", "string")
                    }
                },
                required = listOf("source","keyword")
            ),
        ) { request ->
            val source = request.arguments?.get("source")!!.jsonPrimitive.content
            val keyword = request.arguments!!["keyword"]!!.jsonPrimitive.content
            val website = AnimeRss.Website.entries.find { it.name.equals(source,true) }
            if (website == null) {
                return@addTool CallToolResult(content = listOf(TextContent("Source '$source' Unavailable")), isError = true)
            }
            val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val result = otaku.animeRss.getRssContent(website, keyword)
            val list = ListAnime(result.url,result.channel.items
                .mapIndexed{idx,item-> AnimeItem(idx.toString(),item.title,item.pubDate?.format(pattern)?:"unknown") }
                .take(8))
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(list)))
            )
        }

        server.addTool(
            name = "add-animed-worker",
            description = "为Animed服务器添加或更新一个动画监听程序配置".trimIndent(),
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("id") {
                        put("type", "string")
                    }
                    putJsonObject("rss") {
                        put("type", "string")
                    }
                    putJsonObject("immediately") {
                        put("type","boolean")
                    }
                    putJsonObject("startEpisode") {
                        put("type","number")
                    }
                    putJsonObject("finalEpisode") {
                        put("type","number")
                    }
                    putJsonObject("refreshInterval") {
                        put("type","number")
                    }
                    putJsonObject("titles") {
                        put("type","array")
                        putJsonObject("items"){
                            put("type","string")
                        }
                    }
                    putJsonObject("downloader") {
                        put("type","string")
                    }
                    putJsonObject("downloadPath") {
                        put("type","string")
                    }
                },
                required = listOf("id","rss","titles","downloader","downloadPath")
            ),
        ){ request->
            val args = request.arguments
            if (args==null){
                io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
                return@addTool CallToolResult(content = listOf(TextContent("Argument is empty")), isError = true)
            }

            val id = args["id"]!!.jsonPrimitive.content
            val rss = args["rss"]!!.jsonPrimitive.content
            val immediately = args["immediately"]?.jsonPrimitive?.boolean
            val startEpisode = args["startEpisode"]?.jsonPrimitive?.doubleOrNull
            val finalEpisode = args["finalEpisode"]?.jsonPrimitive?.doubleOrNull
            val refreshInterval = args["refreshInterval"]?.jsonPrimitive?.longOrNull
            val titles = args["titles"]!!.jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
            val downloader = args["downloader"]!!.jsonPrimitive.content
            val downloadPath = args["downloadPath"]!!.jsonPrimitive.content


            if (titles.isEmpty()){
                return@addTool CallToolResult(content = listOf(TextContent("Titles is empty")), isError = true)
            }
            if (otaku.getDownloader(downloader)==null){
                return@addTool CallToolResult(content = listOf(TextContent("Downloader '$downloader' does not exists")), isError = true)
            }

            val config = AnimeConfig(
                id = id,
                rss = rss,
                immediately = immediately?:false,
//                immediately = false,
                startEpisode = startEpisode?:-1.0,
                finalEpisode = finalEpisode,
                refreshInterval = refreshInterval?:3600,
                titles = titles,
                downloader =  downloader,
                downloadPath =  downloadPath
            )

            try {
                otaku.config.addAnime(config);

                CallToolResult(
                    content = listOf(TextContent(Json.encodeToString(config)))
                )
            } catch (e: Exception) {
                CallToolResult(
                    content = listOf(TextContent(e.message.orEmpty())),
                    isError = true
                )
            }
        }

        return server
    }

    fun stateAsConfig():McpConfig{
        return McpConfig(
            isOpen(), host!!, port!!
        )
    }

    fun isOpen():Boolean{
        return server!=null
    }

    fun start(host:String,port:Int){
        val serverSessions = ConcurrentMap<String, ServerSession>()
        val mcpServer = configureServer()
        server = embeddedServer(CIO, host = host, port = port){
            install(SSE)
            routing {
                sse("/sse") {
                    val transport = SseServerTransport("/message", this)
                    val serverSession = mcpServer.createSession(transport)
                    serverSessions[transport.sessionId] = serverSession

                    serverSession.onClose {
                        println("Server session closed for: ${transport.sessionId}")
                        serverSessions.remove(transport.sessionId)
                    }
                    awaitCancellation()
                }
                post("/message") {
                    val sessionId: String? = call.request.queryParameters["sessionId"]
                    if (sessionId == null) {
                        call.respond(HttpStatusCode.BadRequest, "Missing sessionId parameter")
                        return@post
                    }

                    val transport = serverSessions[sessionId]?.transport as? SseServerTransport
                    if (transport == null) {
                        call.respond(HttpStatusCode.NotFound, "Session not found")
                        return@post
                    }

                    transport.handlePostMessage(call)
                }
            }
        }
        this.host = host
        this.port = port
        server!!.start(wait = false)
        logger.info("mcp server started on $host:$port")
    }


    fun stop(){
        server?.stop(4500,5000)
        server=null
        logger.info("mcp server stopped")
    }


}