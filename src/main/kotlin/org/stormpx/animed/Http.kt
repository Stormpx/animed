package org.stormpx.animed

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration
import java.util.Objects
import java.util.concurrent.Executors

class Http {
    companion object{
        private val logger: Logger = LoggerFactory.getLogger(Http::class.java)
        val proxySelector: MutableProxySelector = MutableProxySelector()
        val client:HttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .proxy(proxySelector)
            .connectTimeout(Duration.ofSeconds(30))
            .executor(Executors.newSingleThreadExecutor())
            .build()
    }

    class MutableProxySelector : ProxySelector() {

        private var proxies : MutableList<Proxy>?=null

        private var prevUris : List<String>?=null

        private fun defaultPort(protocol:String ):Int {
            return if ("http".equals(protocol,true)) {
                80
            } else if ("https".equals(protocol,true)) {
                443
            } else {
                -1
            }
        }
        private fun toProxy(uri:URI):Proxy{
            val type = Proxy.Type.HTTP
            val address = InetSocketAddress.createUnresolved(uri.host,if (uri.port==-1){defaultPort(uri.scheme)}else{uri.port})
            return Proxy(type,address)
        }

         fun setProxies(uris:List<String>){
             if (Objects.equals(prevUris,uris)){
                 return
             }
             prevUris=uris
            proxies = uris.map { URI.create(it) }
                .filter { it.scheme.startsWith("http",true)}
                .map { toProxy(it) }
                .toMutableList()
        }

        override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
            //ignore
        }
        override fun select(uri: URI?): MutableList<Proxy> {
            if(proxies?.isEmpty() != false){
                return arrayListOf(Proxy.NO_PROXY)
            }
            return proxies as MutableList<Proxy>
        }

    }
}