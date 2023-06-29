package org.stormpx.animed.aria2

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import org.stormpx.animed.Http
import org.stormpx.animed.JsonRpcRequest
import org.stormpx.animed.JsonRpcResult
import java.io.InputStream
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64
import java.util.UUID

/**
 * simple impl
 */
class Aria2Client(
    private val serverUri:URI,
    private val token:String?,
    ) {

    private var secretToken:String? =null
    init {
        token?.isNotBlank().let {
            secretToken="token:$token";
        }

    }

    fun addUri(uri:String,downloadPath:String): JsonRpcResult<String, Unit> {

        val id = UUID.randomUUID().toString().replace("-","")
        val json=Json.encodeToString(
            JsonRpcRequest.serializer(),
            JsonRpcRequest(
                id = id,
                method = "aria2.addUri",
                params = arrayOf(
                    if (secretToken!=null) JsonPrimitive(secretToken) else null,
                    JsonArray(listOf(JsonPrimitive(uri))),
                    JsonObject(mapOf("dir" to JsonPrimitive(downloadPath))),
                ).filterNotNull().toTypedArray(),
            )
        )
        val response = Http.client
            .send(HttpRequest.newBuilder(serverUri)
                .timeout(Duration.ofMinutes(1))
                .header("content-type","application/json;charset=uft-8")
                .POST(BodyPublishers.ofString(json)).build(),BodyHandlers.ofInputStream());
        if (response.statusCode()!=200){
//            println(response.body().readAllBytes().toString(StandardCharsets.UTF_8))
            throw RuntimeException("request aria2.addUri return statuscode ${response.statusCode()}");
        }

        return Json.decodeFromStream(JsonRpcResult.serializer(String.serializer(),Unit.serializer()),response.body())

    }
    fun addTorrent(inputStream:InputStream,downloadPath:String):JsonRpcResult<String,Unit>{

        val torrent = Base64.getEncoder().encodeToString(inputStream.readAllBytes())
        val id = UUID.randomUUID().toString().replace("-","")
        val json=Json.encodeToString(
            JsonRpcRequest.serializer(),
            JsonRpcRequest(
                id = id,
                method = "aria2.addTorrent",
                params = arrayOf(
                    if (secretToken!=null) JsonPrimitive(secretToken) else null,
                    JsonPrimitive(torrent),
                    JsonArray(listOf()),
                    JsonObject(mapOf("dir" to JsonPrimitive(downloadPath))),
                ).filterNotNull().toTypedArray(),
            )
        )
        val response = Http.client
            .send(HttpRequest.newBuilder(serverUri)
                .timeout(Duration.ofMinutes(1))
                .header("content-type","application/json;charset=uft-8")
                .POST(BodyPublishers.ofString(json)).build(),BodyHandlers.ofInputStream());

        if (response.statusCode()!=200){
            throw RuntimeException("request aria2.addTorrent return statuscode ${response.statusCode()}");
        }

        return Json.decodeFromStream(JsonRpcResult.serializer(String.serializer(),Unit.serializer()),response.body())

    }

}