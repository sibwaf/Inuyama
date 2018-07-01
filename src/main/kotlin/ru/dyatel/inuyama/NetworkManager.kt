package ru.dyatel.inuyama

import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import io.objectbox.Box
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_
import java.io.IOException

class NetworkManager(override val kodein: Kodein) : KodeinAware {

    private val wifiManager by kodein.instance<WifiManager>()
    private val networkBox by kodein.instance<Box<Network>>()

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

}

class UntrustedNetworkException : IOException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
