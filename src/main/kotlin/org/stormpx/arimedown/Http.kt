package org.stormpx.arimedown

import java.net.http.HttpClient

class Http {
    companion object{
        val client:HttpClient = HttpClient.newHttpClient()
    }

}