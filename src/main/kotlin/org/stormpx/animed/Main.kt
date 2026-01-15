import ch.qos.logback.classic.Level
import com.charleskorn.kaml.MissingRequiredPropertyException
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import jakarta.mail.Message
import kotlinx.serialization.modules.EmptySerializersModule
import org.simplejavamail.api.email.Recipient
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.animed.*
import org.stormpx.animed.download.Aria2Downloader
import org.stormpx.animed.download.Downloader
import java.nio.charset.StandardCharsets
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.io.path.Path
import kotlin.io.path.exists


fun main(args: Array<String>) {

    if (args.isEmpty()){
        error("no config specified")
    }
    System.setProperty("jdk.httpclient.keepalive.timeout","0")
    val configPath:String= args[0]
    var id = 1
    val threadPool = Executors.newScheduledThreadPool(1) {
        val t= Thread(it)
        t.name="Otaku-Thread-${id++}"
        t
    }
    try {
        DieOtaku(configPath,threadPool).start()

    } catch (e: Exception) {
        e.printStackTrace()
        error(e)
    }


}

class DieOtaku (
    configPath:String,
    private val threadPool: ScheduledExecutorService,
    ) : AnimedContext{

    companion object{
        private val logger: Logger = LoggerFactory.getLogger(DieOtaku::class.java)
        private const val delay: Long = 10
    }
    val animeRss:AnimeRss = AnimeRss()
    val config:Config = Config(Path(configPath))
    private val workers = ArrayList<Worker>()
    private val downloader = HashMap<String,Downloader>()
    private var mailer:Mailer? = null
    private var users:Array<UserConfig> = emptyArray()
    private var mcpServer:McpServer= McpServer(this)

    private fun assembleWorker(appConfig: AppConfig){
        val animeConfigs = appConfig.anime

        animeConfigs.distinctBy { it.id }
            .filter { workers.isEmpty()||workers.any { worker ->  worker.isOptionChange(it) }||workers.all { worker -> !worker.isSameId(it.id) } }
            .forEach{ it ->
                val worker = try {
                    val worker= Worker(appConfig.path(),it, this)
                    val oldWorkers = workers.filter { existsWorker -> existsWorker.isOptionChange(it) }
                    if (oldWorkers.isNotEmpty()){
                        logger.info("worker [${worker.id()}] config changed. try restart..")
                    }
                    oldWorkers.forEach{ it.cancel() }
                    workers.removeAll(oldWorkers.toSet())
                    worker.schedule()

                    workers.add(worker)
                    worker
                }catch (e:Exception){
                    logger.error(e.message)
                    return
                }
                logger.info("worker [${worker.id()}] started..")
                if (it.immediately)
                    threadPool.execute(worker::start)

            }

        val canceledWorkers = workers.filter { worker -> !animeConfigs.any { worker.isSameId(it.id)  } }
        canceledWorkers.forEach{ worker -> worker.cancel() }
        workers.removeAll(canceledWorkers.toSet())

    }

    fun newDownloader(option:DownloaderConfig):Downloader{
        return Aria2Downloader(option)
    }

    fun buildMailer(config:AppConfig){

        config.email?.let {
            mailer = MailerBuilder
                .withSMTPServer(it.host,it.port,it.username,it.password)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .clearEmailValidator()
                .withDebugLogging(config.debug)
                .withThreadPoolSize(1)
                .buildMailer()
        }

    }

    private fun setLogLevel(level:Level){
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        root.level = level
    }
    private fun handleNewConfig(config:AppConfig){
        if (!config.path().exists()){
            Files.createDirectories(config.path())
        }
        Animed.animed.markDir(config.path())
        setLogLevel(if(config.debug){Level.DEBUG}else{Level.INFO})
        users = config.users?: emptyArray()
        config.proxies?.let { Http.proxySelector.setProxies(it.asList()) }

        val latestMcp = this.config.latestConfig?.mcp
        val mcp = config.mcp
        if (latestMcp!=mcp){
            if (mcp==null){
                if (mcpServer.isOpen())
                    mcpServer.stop()
            }else{
                if (mcpServer.isOpen()){
                    if (mcp!=mcpServer.stateAsConfig()){
                        mcpServer.stop()
                    }
                }
                if (!mcpServer.isOpen()&&mcp.enable==true){
                    mcpServer.start(host = mcp.host?:"127.0.0.1", port = mcp.port?:3001)
                }
            }
        }

        this.config.latestConfig=config
    }

    fun start(){
        val config = config.readConfig()

        handleNewConfig(config)

        config.downloader.forEach {
            downloader[it.id]=newDownloader(it)
        }

        buildMailer(config)

        threadPool.scheduleWithFixedDelay({
            try {
                val appConfig = this.config.readConfig()
                handleNewConfig(appConfig)
                assembleWorker(appConfig)

            } catch (e: MissingRequiredPropertyException) {
                logger.error("unable read config because: at line ${e.location.line} column ${e.location.column} ${e.message}")
            }catch (e: Exception){
                logger.error(e.toString())
            }
        }, delay, delay,TimeUnit.SECONDS)

        logger.info("Animed started.")

        assembleWorker(config)

    }

    override fun getDownloader(id: String): Downloader? {
        return downloader[id]
    }

    override fun scheduler(): ScheduledExecutorService {
        return threadPool
    }

    override fun notice(message: AnimedContext.AnimeMessage) {
        if (mailer==null){
            return
        }

        val mailer = mailer

        var recipients = message.users.distinct()
            .mapNotNull { name -> users.findLast { Objects.equals(name,it.name) } }
            .map { Recipient(it.name,it.email, Message.RecipientType.TO) }

        if (recipients.isEmpty()) {
            val appConfig= this.config.latestConfig
            val target= appConfig?.defaultTarget?:return
            recipients= listOf((users.findLast { Objects.equals(it.name,target) }?.run { Recipient(name, email, Message.RecipientType.TO) }?:return))
        }

        val username = mailer?.serverConfig?.username ?: return

        try {
            mailer.sendMail(
                EmailBuilder.startingBlank()
                    .from(message.from, username)
                    .to(recipients)
                    .withSubject(message.subject)
                    .withHTMLText(message.content)
                    .buildEmail()
            )

            logger.info("${message.from} send email to ${recipients.map { it.name }} success")
        } catch (e: Exception) {
            logger.error("",e)
        }


    }

}

