package sibwaf.inuyama.app.common

import android.content.Context
import org.jsoup.Connection

interface RemoteService {

    // todo: that has to go
    val networkManager: NetworkManager

    fun getName(context: Context): String
    suspend fun checkConnection(): Boolean

    fun getHttpClient(trustedOnly: Boolean) = networkManager.getHttpClient(trustedOnly)

    @Deprecated("Use getHttpClient() instead")
    fun createConnection(url: String, trustedOnly: Boolean): Connection {
        @Suppress("DEPRECATION")
        return networkManager.createJsoupConnection(url, trustedOnly)
    }
}
