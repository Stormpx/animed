package org.stormpx.arimedown

import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.Executors

class Http {
    companion object{
        val client:HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .executor(Executors.newSingleThreadExecutor())
            .build()
    }

}