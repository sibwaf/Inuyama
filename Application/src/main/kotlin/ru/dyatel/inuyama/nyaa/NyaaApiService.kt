package ru.dyatel.inuyama.nyaa

import android.content.Context
import okhttp3.Request
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.SERVICE_NYAA
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.sibwaf.inuyama.common.api.NyaaApi
import ru.sibwaf.inuyama.common.utilities.await
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.ProxyableRemoteService

class NyaaApiService(override val kodein: Kodein) : KodeinAware, ProxyableRemoteService {

    override val serviceId = SERVICE_NYAA

    override val networkManager by instance<NetworkManager>()
    private val api = NyaaApi()

    override fun getName(context: Context): String = context.getString(R.string.module_nyaa)

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = Request.Builder().url(api.host).build()
            val response = getHttpClient(false).newCall(request).await()
            return response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun query(query: String): List<NyaaTorrent> {
        val torrents = api.query(query, getHttpClient(false))
        return torrents.map {
            NyaaTorrent(
                    id = it.id.toLong(), // TODO
                    title = it.title,
                    hash = it.hash,
                    updateDatetime = it.lastUpdate,
                    dispatched = false
            )
        }
    }

    suspend fun getMagnet(torrent: NyaaTorrent): String {
        return api.getMagnet(torrent.id.toString(), getHttpClient(false))
    }
}
