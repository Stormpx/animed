package org.stormpx.animed

import DieOtaku
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

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


        // config resource
        server.addResource(
            uri = "file://config.yaml",
            name = "Config",
            description = "Get Config of Animed Server",
            mimeType = "text/x-yaml",

            ) { request ->
            ReadResourceResult(
                contents = listOf(
                    TextResourceContents(otaku.config.readPlain(), request.uri, "text/x-yaml")
                )
            )
        }

        server.addTool(
            name = "get-animed-config-tool",
            description = """
                获取Animed配置工具
                获取Animed服务器的配置文件,没有参数,返回的内容是yaml格式。
                # Animed 配置文档说明

                ## 基础架构
                - 配置格式：YAML
                - 全局存储路径：`data_path: /path`（必须存在目录）
                - 功能模块：`监听动画配置(anime)` + `下载器配置(downloader)`

                ## 动画监听配置规范
                **参数层级**: `anime:`
                  - `id`: 唯一动画标识符（如：`witch-from-mercury`）
                  - `rss`: RSS源地址（支持域名包括 dmhy.org/nyaa.si/bangumi.moe）
                  - `immediately`: 是否立即解析（布尔值，默认false）
                  - `start_episode`: 起始检测集数（>=此值时触发下载）
                  - `final_episode`: 终止检测集数（<=此值时停止检测） 
                  - `refresh_interval`: 检测间隔秒数（建议≥1800秒）
                  - `titles:`
                    - 已成功匹配的标题参考列表
                    - 格式示例：`【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][01][1080p][简体][招募翻译校对]`
                  - `patterns:`
                    - 使用占位符`#ep#`标记集数位置
                    - 示例：`【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][#ep#][1080p][简体][招募翻译校对]`
                  - `downloader`: 指定下载器ID（需要与download配置一致）
                  - `download_path`: 下载路径（支持相对路径）

                ## 下载器配置规范 
                **参数层级**: `downloader:`
                  - `id`: 下载器唯一ID（如：`aria2`）
                  - `type`: 只支持`aria2`
                  - `uri`: aria2的JSON-RPC地址（默认`http://127.0.0.1:6800/jsonrpc`）
                  - `token`: RPC密钥（可选）
                  - `downloadPath`: 下载器的基础路径(当动画监听的下载路径指定了相对路径时，就是基于该路径来进行'相对')

                ## 配置文件验证规则
                1. YAML缩进必须使用2空格
                2. 每个anime配置必须有id/rss/(titles/rules二选一)/refresh_interval/downloader/download_path
                3. downloader配置必须包含type/uri基础参数
                4. 正则表达式元字符需要转义（如`[]`需写成`\[\]`）

                ## 典型配置示例
                ```yaml
                anime:
                  - id: Lycoris-Recoil
                    rss: https://dmhy.org/topics/rss/rss.xml?keyword=Lycoris+Recoil
                    immediately: true
                    start_chapter: 3
                    refresh_interval: 1000
                    titles:
                      - 【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][01][1080p][简体][招募翻译校对]
                    rules:
                      - 【喵萌奶茶屋】★07月新番★[莉可丽丝/Lycoris Recoil][#ep#][1080p][简体][招募翻译校对]
                    downloader: aria2
                    download_path: /downloads/VIDEO/anima/Lycoris-Recoilssss/Season 1
                 
                 工具返回的配置直接原样展示即可，不能擅自整理、省略、归纳、总结、分类。
            """.trimIndent(),
            inputSchema = Tool.Input()
        ) { request ->
            CallToolResult(
                content = listOf(TextContent(otaku.config.readPlain()))
            )
        }


        server.addTool(
            name = "list-anime-tool",
            description = """
                搜索番剧工具
                这工具的任务是基于用户指定的动漫资源网站和提供的关键词去搜索可下载番剧。
                这工具只有两个参数 `source` 和 `keyword`
                source是一个枚举值 现在只有 ['dmhy','mikan','bangumi','nyaasi']
                    dmhy 的别称有: '动漫花园','DMHY','冻鳗花园','dmhy.org'等
                    mikan 的别称有: '蜜柑计划','Mikan Project','mikanani.me'等
                    bangumi 的别称有： 'Bangumi Moe','bangumi.moe','BANGUMI','番组'等
                    nyaasi 的别称有: 'nyaa.si'等
                keyword则是完全由用户提供的番剧名称关键词，不要添油加醋，用户说什么传什么
                当用户说类似'帮我去WWW搜索XXXX'的话时 将WWW(动漫资源网站)和XXXX(番剧名称)转换为本工具可接受的参数传入。
                比如: '帮我去动漫花园搜索攻壳机动队' 动漫花园将会被转换为dmhy作为参数source,攻壳机动队会被作为参数keyword。
                这工具只是去对应网站搜索并展示结果,不会触发真正的下载。
                必须在用户明确要求搜索时才去搜索。
                返回的结果是JSON,比如:
                {
                    "url": "xxxxx", //搜索的网址
                    animes: [
                        {
                            "id": "id",
                            "title": "[ANi]  小市民系列 第二季 - 14 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]",
                            "pubTime": "2025-01-01 00:00:00"
                        }
                    ]
                }
                为方便后续用户指定相应的剧集标题以使用其他工具，只需要将animes数组里面的内容简单整理包含id一行一行展示即可，不能省略、归纳、总结、分类。
                比如可以这样展示结果:
                搜索结果网址：https://bangumi.moe/rss/search/小市民
                当前在 bangumi.moe 网站使用关键词“小市民”的搜索结果如下（共3条）：
                | ID| 标题| 发布时间|
                |:---|:---|:---|
                |a|[黒ネズミたち] 小市民系列 第二季 / Shoushimin Series 2nd Season - 14 (CR 1920x1080 AVC AAC MKV)| 2025-01-01 00:00:01|
                |b|[ANi] 小市民系列 第二季 - 14 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]| 2025-01-01 00:00:01|
                |c|[北宇治字幕组] 小市民系列 / Shoushimin Series [13][WebRip][HEVC_AAC][简日内嵌]| 2025-01-01 00:00:01|
            """.trimIndent(),
            inputSchema = Tool.Input(
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
            val source = request.arguments["source"]!!.jsonPrimitive.content
            val keyword = request.arguments["keyword"]!!.jsonPrimitive.content
            val website = AnimeRss.Website.entries.find { it.name.equals(source,true) }
            if (website == null) {
                return@addTool CallToolResult(content = listOf(TextContent("Source '$source' Unavailable")), isError = true)
            }
            val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val result = otaku.animeRss.getRssContent(website, keyword)
            val list = ListAnime(result.url,result.channel.items
                .mapIndexed{idx,item-> AnimeItem(idx.toString(),item.title,item.pubDate?.format(pattern)?:"unknown") }
                .take(15))
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(list)))
            )
        }

        server.addTool(
            name = "add-anime-worker-tool",
            description = """
                这个工具的任务是基于用户的需求为Animed服务器添加或更新一个新的动画监听程序配置。
                使用这个工具前务必确认已经通过 '获取Animed配置工具' 确认过现有配置，因为使用该工具的部分参数需要基于'获取Animed配置工具'的风格/值确定
                使用这个工具前务必确认已经通过 '搜索番剧工具' 搜索过番剧，因为使用该工具的许多参数基于'搜索番剧工具'的返回结果得到,
                如果你不确定是否使用过'搜索番剧工具'则不能调用该工具并提醒用户先使用'搜索番剧工具'搜索。
                使用这个工具有几个参数 ['id','rss','immediately','startEpisode','finalEpisode','refreshInterval','titles','downloader','downloadPath']
                id: 动画监听程序配置的id, 这个值通常是你基于用户希望下载的番剧名称转换为由英文字母/数字组成长度不超过20的字符串。
                rss: 动画程序监听的rss源地址, 这个值通过 '搜索番剧工具' 返回的JSON中的`url`字段得到,不要添油加醋原样传入即可。
                immediately: 控制动画监听程序是否在刷新配置后立即进行读取匹配,除非用户特意要求不要立即触发读取匹配，否则这个值默认传true。
                startEpisode: 控制动画监听程序匹配时至少要大于该配置才进行下载，如果用户没有明确要求就不用传。
                finalEpisode: 控制动画监听程序在到达该集数后不再进行读取，如果用户没有明确要求就不用传。
                refreshInterval: 控制动画监听程序执行读取匹配的间隔(秒),如果用户没有明确要求则生成一个2-12小时的随机数传入。
                titles: 动画监听程序进行匹配的剧集标题，类型为字符串JsonArray，该数组获取方式是：
                    用户会指定'搜索番剧工具'返回结果中的ID，其ID所对应的剧集标题，原样传入即可，不能添油加醋。用户指定了多个ID则多个标题添加进该数组。
                downloader: 动画监听程序的下载器ID，传入的下载器ID必须已存在，如果不确定是否存在可通过'获取Animed配置工具'得知，如果不能确认下载器ID的值则不能调用本工具。
                    该值用户如果没有指定则可以通过'获取Animed配置工具'返回的配置中其他动画监听程序中使用最多的下载器ID获得。原样传入即可，不能添油加醋。
                downloadPath: 动画监听程序的下载路径，可以传相对路径。该值用户如果没有指定则可以参考'获取Animed配置工具'返回的配置中其他动画监听程序的下载路径风格，
                    如果你决定使用绝对路径则必须遵循其他监听程序的风格，如果使用相对路径则使用 `./xxx`(xxx=动画名称) 格式即可。
                你在查看现有配置的时候可能会看到上述参数是以下划线风格定义的，忽略并坚持使用本工具定义的驼峰风格。
                工具调用成功会返回新配置的JSON格式数据，原样展示给用户即可,不能擅自整理、省略、归纳、总结、分类。
                如果调用失败则在告知用户错误原因后终止对话，不能再次尝试。
                本工具有副作用，你必须在用户明确要求添加一个新的监听程序才可调用本工具，在调用前必须仔细检查参数是否符合上述规范/定义，如果任何参数不确认你可以中止调用本工具并要求用户提供更具体的描述。
            """.trimIndent(),
            inputSchema = Tool.Input(
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
            val id = request.arguments["id"]!!.jsonPrimitive.content
            val rss = request.arguments["rss"]!!.jsonPrimitive.content
            val immediately = request.arguments["immediately"]?.jsonPrimitive?.boolean
            val startEpisode = request.arguments["startEpisode"]?.jsonPrimitive?.doubleOrNull
            val finalEpisode = request.arguments["finalEpisode"]?.jsonPrimitive?.doubleOrNull
            val refreshInterval = request.arguments["refreshInterval"]?.jsonPrimitive?.longOrNull
            val titles = request.arguments["titles"]!!.jsonArray.map { it.jsonPrimitive.content }.toTypedArray()
            val downloader = request.arguments["downloader"]!!.jsonPrimitive.content
            val downloadPath = request.arguments["downloadPath"]!!.jsonPrimitive.content


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
                    content = listOf(TextContent(e.message)),
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
        server = embeddedServer(CIO, host = host, port = port){
            mcp {
                return@mcp configureServer()
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