package sibwaf.inuyama.app.common

import okhttp3.OkHttpClient
import org.jsoup.Connection
import java.net.InetAddress

interface NetworkManager {

    val broadcastAddress: InetAddress?
    val isNetworkTrusted: Boolean

    fun refreshNetworkList()

    fun getHttpClient(trustedOnly: Boolean, serviceId: Long? = null): OkHttpClient

    @Deprecated("Use getHttpClient() instead")
    fun createJsoupConnection(url: String, trustedOnly: Boolean): Connection

    @Deprecated("Use getHttpClient() instead")
    fun createProxiedJsoupConnection(url: String, trustedOnly: Boolean, serviceId: Long): Connection
}
