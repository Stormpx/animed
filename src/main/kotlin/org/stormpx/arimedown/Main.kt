import com.charleskorn.kaml.MissingRequiredPropertyException
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
import org.stormpx.arimedown.*
import org.stormpx.arimedown.download.Aria2Downloader
import org.stormpx.arimedown.download.Downloader
import java.nio.file.Files
import java.util.Objects
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path



fun main(args: Array<String>) {

    if (args.isEmpty()){
        error("no config specified")
    }

    val configPath:String= args[0]

    val threadPool = Executors.newScheduledThreadPool(1)
    try {
        DieOtaku(configPath,threadPool).start()

    } catch (e: Exception) {
        error(e)
    }


}

class DieOtaku (
    val configPath:String,
    val threadPool: ScheduledExecutorService,
    ) : AnimedContext{

    companion object{
        private val logger: Logger = LoggerFactory.getLogger(DieOtaku::class.java)
        private const val delay: Long = 10
        val yaml = Yaml(EmptySerializersModule, YamlConfiguration(
            encodeDefaults = false,
            strictMode = false
        ))
    }
    private var latestConfig:AppConfig?=null;
    private val workers = ArrayList<Worker>()
    private val downloader = HashMap<String,Downloader>()
    private var mailer:Mailer? = null
    private var users:Array<UserConfig> = emptyArray()

    private fun readConfig():AppConfig{
        return yaml.decodeFromStream(AppConfig.serializer(),Files.newInputStream(Path(configPath)))
    }


    private fun assembleWorker(appConfig: AppConfig){
        val animeConfigs = appConfig.anime;

        animeConfigs.distinctBy { it.id }
            .filter { workers.isEmpty()||workers.any { worker ->  worker.isOptionChange(it) }||workers.all { worker -> !worker.isSameId(it.id) } }
            .forEach{ it ->
                val worker = try {
                    val worker= Worker(appConfig.path(),it, this)
                    val oldWorkers = workers.filter { existsWorker -> existsWorker.isOptionChange(it) }
                    if (oldWorkers.isNotEmpty()){
                        logger.info("worker [${worker.id()}] config change. try restart..");
                    }
                    oldWorkers.forEach{ it.cancel() }
                    workers.removeAll(oldWorkers.toSet())
                    worker.schedule()

                    workers.add(worker)
                    worker
                }catch (e:Exception){
                    logger.error(e.message);
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

    fun start(){
        val config = readConfig()
        latestConfig=config
        config.downloader.forEach {
            downloader[it.id]=newDownloader(it)
        }

        buildMailer(config)

        users = config.users?: emptyArray()

        threadPool.scheduleWithFixedDelay({
            try {
                val appConfig = readConfig()
                latestConfig=appConfig
                users = config.users?: emptyArray()
                assembleWorker(appConfig)
            } catch (e: MissingRequiredPropertyException) {
                logger.error("unable read config because: at line ${e.location.line} column ${e.location.column} ${e.message}")
            }catch (e: Exception){
                logger.error(e.message)
            }
        }, delay, delay,TimeUnit.SECONDS)

        logger.info("animed started.")

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
            val appConfig= latestConfig
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

