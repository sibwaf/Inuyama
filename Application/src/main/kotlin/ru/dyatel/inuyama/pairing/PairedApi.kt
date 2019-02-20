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
import ru.sibwaf.inuyama.common.STATUS_OK

class PairedApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private val gson by instance<Gson>()
    private val pairingManager by instance<PairingManager>()

    override val networkManager by instance<NetworkManager>()

    private val address: String
        get() = pairingManager.findPairedServer()
                ?.let { "http://${it.address.hostAddress}:${it.port}" }
                ?: throw IllegalStateException()

    override fun getName(context: Context) = context.getString(R.string.module_pairing)!!

    private fun createConnection(url: String) = createConnection(url, true).ignoreContentType(true)

    private inline fun <reified T : ApiResponse> get(url: String, noinline init: Connection.() -> Unit = {}): T {
        return createConnection(url)
                .apply(init)
                .get()
                .let { gson.fromJson(it.text()) }
    }

    private inline fun <reified T : ApiResponse> post(url: String, noinline init: Connection.() -> Unit = {}): T {
        return createConnection(url)
                .apply(init)
                .post()
                .let { gson.fromJson(it.text()) }
    }

    override fun checkConnection(): Boolean {
        return try {
            checkConnection(address)
        } catch (e: Exception) {
            false
        }
    }

    fun checkConnection(address: String): Boolean {
        return try {
            get<ApiResponse>("$address/ping").status == STATUS_OK
        } catch (e: Exception) {
            false
        }
    }
}