package ru.dyatel.inuyama

import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import io.objectbox.Box
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_
import ru.dyatel.inuyama.model.ProxyBinding
import java.io.IOException

class NetworkManager(override val kodein: Kodein) : KodeinAware {

    private val wifiManager by kodein.instance<WifiManager>()
    private val networkBox by kodein.instance<Box<Network>>()
    private val proxyBindingBox by kodein.instance<Box<ProxyBinding>>()

    fun isNetworkTrusted(): Boolean {
        val connection = wifiManager.connectionInfo
        if (connection.supplicantState != SupplicantState.COMPLETED) {
            return false
        }

        val known = networkBox.query()
                .equal(Network_.bssid, connection.bssid)
                .build()
                .find()

        if (known.any()) {
            return known.single().trusted
        }

        networkBox.put(Network(name = connection.ssid, bssid = connection.bssid))
        return false
    }

    fun createConnection(url: String, serviceId: Long): Connection {
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
