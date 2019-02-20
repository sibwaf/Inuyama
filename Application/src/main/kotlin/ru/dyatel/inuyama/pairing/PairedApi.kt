package ru.dyatel.inuyama.pairing

import android.content.Context
import com.google.gson.Gson
import org.jsoup.Connection
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.utilities.fromJson
import ru.sibwaf.inuyama.common.ApiResponse
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest

class PairedApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private val gson by instance<Gson>()

    private val pairingManager by instance<PairingManager>()
    override val networkManager by instance<NetworkManager>()

    private val address: String
        get() = pairingManager.findPairedServer()
                ?.let { "http://${it.address.hostAddress}:${it.port}" }
                ?: throw IllegalStateException()

    private inline fun <reified T : ApiResponse> request(
            url: String,
            method: Connection.Method,
            crossinline init: Connection.() -> Unit
    ): T {
        return createConnection("${address!!}$url", true)
                .ignoreContentType(true)
                .apply(init)
                .method(method)
                .execute()
                .let { gson.fromJson(it.body()) }
    }

    private inline fun <reified T : ApiResponse> get(
            url: String,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.GET, init)
    }

    private inline fun <reified T : ApiResponse> post(
            url: String,
            data: Any? = null,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.POST) {
            init()

            // TODO: encode

            if (data != null) {
                requestBody(gson.toJson(data))
            }
        }
    }

    fun ping() {
        get<ApiResponse>("/ping")
    }

    fun downloadTorrent(magnet: String, path: String) {
        post<ApiResponse>("/download-torrent", TorrentDownloadApiRequest(magnet, path))
    }

    override fun getName(context: Context) = context.getString(R.string.module_pairing)!!

    override fun checkConnection(): Boolean {
        return try {
            ping()
            true
        } catch (e: Exception) {
            false
        }
    }
}