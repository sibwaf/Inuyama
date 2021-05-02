package sibwaf.inuyama.app.common

import org.jsoup.Connection

interface ProxyableRemoteService : RemoteService {

    val serviceId: Long

    override fun getHttpClient(trustedOnly: Boolean) = networkManager.getHttpClient(trustedOnly, serviceId)

    @Deprecated("Use getHttpClient() instead")
    override fun createConnection(url: String, trustedOnly: Boolean): Connection {
        @Suppress("DEPRECATION")
        return networkManager.createProxiedJsoupConnection(url, trustedOnly, serviceId)
    }
}