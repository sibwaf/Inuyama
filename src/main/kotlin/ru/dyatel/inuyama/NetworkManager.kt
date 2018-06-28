package ru.dyatel.inuyama

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.model.Network
import ru.dyatel.inuyama.model.Network_

class NetworkManager(override val kodein: Kodein) : BroadcastReceiver(), KodeinAware {

    private val wifiManager by kodein.instance<WifiManager>()

    private val boxStore by kodein.instance<BoxStore>()
    private val networkBox by kodein.instance<Box<Network>>()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            boxStore.runInTx {
                val networks = wifiManager.scanResults
                        .map { Network(name = it.SSID, bssid = it.BSSID) }
                        .toMutableSet()

                networkBox.all
                        .forEach { network ->
                            val found = networks.singleOrNull { it.bssid == network.bssid }
                            if (found != null) {
                                found.id = network.id
                                found.trusted = network.trusted
                            } else {
                                networks += network
                            }
                        }

                networkBox.removeAll()
                networkBox.put(networks)
            }
        }
    }

    fun isNetworkTrusted(): Boolean {
        val connection = wifiManager.connectionInfo
        if (connection.supplicantState == SupplicantState.COMPLETED) {
            return networkBox.query()
                    .equal(Network_.bssid, connection.bssid)
                    .equal(Network_.trusted, true)
                    .build()
                    .count() != 0L
        }
        return false
    }

}