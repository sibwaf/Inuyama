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
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.STATUS_OK
import ru.sibwaf.inuyama.common.STATUS_SESSION_ERROR
import ru.sibwaf.inuyama.common.StatefulApiRequest
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import java.net.ConnectException

class PairedApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private data class Session(val address: String, val id: String) // TODO: key

    private val gson by instance<Gson>()

    private val pairingManager by instance<PairingManager>()
    override val networkManager by instance<NetworkManager>()

    private var session: Session? = null

    private inline fun <reified T : ApiResponse> request(
            url: String,
            method: Connection.Method,
            allowNewSession: Boolean,
            crossinline init: Connection.() -> Unit
    ): T {
        if (pairingManager.pairedServer == null) {
            session = null
            throw PairedSessionException("No device is paired")
        }

        var allowNewSession0 = allowNewSession
        if (session == null) {
            bindSession()
            allowNewSession0 = false
        }

        val requester: () -> T = requester@{
            val session = session ?: throw PairedSessionException("No session is bound")

            val response = createConnection("http://${session.address}$url", true)
                    .ignoreContentType(true)
                    .apply(init)
                    .method(method)
                    .execute()
                    .let { gson.fromJson<T>(it.body()) }

            when (response.status) {
                STATUS_OK -> return@requester response
                STATUS_SESSION_ERROR -> throw PairedSessionException()
                else -> throw PairedApiException("Bad status code: ${response.status}")
            }
        }

        return try {
            requester()
        } catch (e: Exception) {
            if (!allowNewSession0) {
                throw e
            }

            if (e !is ConnectException && e !is PairedSessionException) {
                throw e
            }

            bindSession()
            requester()
        }
    }

    private inline fun <reified T : ApiResponse> get(
            url: String,
            allowNewSession: Boolean = true,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.GET, allowNewSession, init)
    }

    private inline fun <reified T : ApiResponse> post(
            url: String,
            data: Any? = null,
            allowNewSession: Boolean = true,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.POST, allowNewSession) {
            init()

            if (data != null) {
                if (data is StatefulApiRequest) {
                    val session = session ?: throw PairedSessionException("No session is bound")

                    data.session = session.id
                    // TODO: encode
                }

                requestBody(gson.toJson(data))
            }
        }
    }

    fun ping() {
        // TODO: is session necessary?
        get<ApiResponse>("/ping")
    }

    fun bindSession() {
        try {
            val server = pairingManager.findPairedServer() ?: throw PairedSessionException("Paired device is not available")

            val address = "${server.address.hostAddress}:${server.port}"
            session = Session(address, "")

            // TODO: create a key, attach device id
            val response = post<BindSessionApiResponse>("/bind-session", BindSessionApiRequest(), false)
            session = Session(address, response.session)
        } catch (e: Exception) {
            session = null

            if (e is PairedSessionException) {
                throw e
            }

            throw PairedSessionException("Failed to create a session", e)
        }
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