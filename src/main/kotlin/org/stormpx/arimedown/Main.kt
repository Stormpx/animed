import com.charleskorn.kaml.MissingRequiredPropertyException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.stormpx.arimedown.AppOption
import org.stormpx.arimedown.DownloaderOption
import org.stormpx.arimedown.JsonRpcRequest
import org.stormpx.arimedown.Worker
import org.stormpx.arimedown.aria2.Aria2Client
import org.stormpx.arimedown.download.Aria2Downloader
import org.stormpx.arimedown.download.Downloader
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path





fun main(args: Array<String>) {

//    val inputPattern = "[Anime Time] Bucchigire! - #chapter# [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]";
//    var title="[Anime Time] Bucchigire! - 15.5 [1080p][HEVC 10bit x265][AAC][Multi Sub] [Weekly]";
//
//    val result= RegexMatcher(inputPattern).match(title)
//    if (result.match){
//        println(result.chapter)
//    }

    val configPath:String= "example.yaml"
//
    val threadPool = Executors.newScheduledThreadPool(1)
//
    DieOtaku(configPath,threadPool)
        .start()


}

class DieOtaku(
    val configPath:String,
    val threadPool: ScheduledExecutorService,
    ){

    companion object{
        private val logger: Logger = LoggerFactory.getLogger(DieOtaku.javaClass);
        private val delay: Long = 10;
        val yaml = Yaml(EmptySerializersModule, YamlConfiguration(
            encodeDefaults = false,
            strictMode = false
        ))
    }


    private val workers = ArrayList<Worker>()
    private val downloader = HashMap<String,Downloader>()

    private fun readConfig():AppOption{
        return yaml.decodeFromStream(AppOption.serializer(),Files.newInputStream(Path(configPath)))
    }


    private fun assembleWorker(appOption: AppOption){
        val animeOptions = appOption.anime;

        animeOptions.distinctBy { it.id }
            .filter { workers.isEmpty()||workers.any { worker ->  worker.isOptionChange(it) } }
            .forEach{ it ->
                val worker= Worker(appOption.path(),it, downloader,threadPool)
                val oldWorkers = workers.filter { existsWorker -> existsWorker.isOptionChange(it) }
                if (oldWorkers.isNotEmpty()){
                    logger.info("worker [${worker.id()}] config change. try restart..");
                }
                oldWorkers.forEach{ it.cancel() }
                workers.removeAll(oldWorkers.toSet())
                worker.schedule()
                logger.info("worker [${worker.id()}] started..")
                workers.add(worker)
                if (it.immediately)
                    worker.start()

            }

    }

    fun newDownloader(option:DownloaderOption):Downloader{
        return Aria2Downloader(option)
    }


    fun start(){
        val option = readConfig()

        option.downloader.forEach {
            downloader[it.id]=newDownloader(it)
        }

        assembleWorker(option)

        threadPool.scheduleWithFixedDelay({
            try {
                val appOption = readConfig()
                assembleWorker(appOption)
            } catch (e: MissingRequiredPropertyException) {
                logger.error("unable read config because: at line ${e.location.line} column ${e.location.column} ${e.message}")
//                e.printStackTrace()
            }
        }, delay, delay,TimeUnit.SECONDS)

    }

}

