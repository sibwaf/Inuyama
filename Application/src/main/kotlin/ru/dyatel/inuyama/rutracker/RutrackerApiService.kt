package ru.dyatel.inuyama.rutracker

import android.content.Context
import okhttp3.Request
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.SERVICE_RUTRACKER
import ru.sibwaf.inuyama.common.api.RutrackerApi
import ru.sibwaf.inuyama.common.utilities.await
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.ProxyableRemoteService
import java.io.IOException

class RutrackerApiService(override val kodein: Kodein) : KodeinAware, ProxyableRemoteService {

    override val serviceId = SERVICE_RUTRACKER

    private val configuration by instance<RutrackerConfiguration>()
    override val networkManager by instance<NetworkManager>()

    private val api = RutrackerApi(configuration.host)

    override fun getName(context: Context): String = context.getString(R.string.module_rutracker)

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = Request.Builder().url(configuration.host).build()
            val response = getHttpClient(false).newCall(request).await()
            return response.use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    suspend fun getMagnet(topic: Long): String {
        return api.getMagnet(topic, getHttpClient(false))
    }
}
