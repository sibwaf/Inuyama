package ru.dyatel.inuyama

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.objectbox.Box
import io.objectbox.kotlin.query
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.kodein.di.generic.on
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_
import ru.dyatel.inuyama.model.ProxyBinding
import java.io.IOException

class NetworkManager(override val kodein: Kodein) : KodeinAware {

    private val context by instance<Context>()
    private val wifiManager by on(context).instance<WifiManager>()
    private val connectivityManager by on(context).instance<ConnectivityManager>()

    private val networkBox by instance<Box<Network>>()
    private val proxyBindingBox by instance<Box<ProxyBinding>>()

    private val currentWifiConnection: WifiInfo?
        get() {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return null

            val wifiEnabled = connectivityManager
                    .getNetworkCapabilities(connectivityManager.activeNetwork)
                    .hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

            if (!wifiEnabled || !networkInfo.isConnected) {
                return null
            }

            return wifiManager.connectionInfo
        }

    val isNetworkTrusted: Boolean
        get() {
            val current = currentWifiConnection ?: return false
            return networkBox.query { equal(Network_.bssid, current.bssid) }.findFirst()?.trusted ?: false
        }

    fun refreshNetworkList() {
        val current = currentWifiConnection

        networkBox.query {
            notEqual(Network_.trusted, true)

            if (current != null) {
                and()
                notEqual(Network_.bssid, current.bssid)
            }
        }.remove()

        if (current != null && networkBox.query { equal(Network_.bssid, current.bssid) }.count() == 0L) {
            networkBox.put(Network(name = current.ssid, bssid = current.bssid))
        }
    }

    fun createProxiedJsoupConnection(url: String, serviceId: Long): Connection {
        val connection = Jsoup.connect(url)

        proxyBindingBox[serviceId]
                ?.proxy
                ?.target
                ?.let { connection.proxy(it.host, it.port) }

        return connection
    }

}

class UntrustedNetworkException : IOException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
