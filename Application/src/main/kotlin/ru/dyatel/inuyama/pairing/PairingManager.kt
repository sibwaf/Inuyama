package ru.dyatel.inuyama.pairing

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.DiscoverRequest
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramSocket
import kotlin.random.Random

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()

    private val preferenceHelper by instance<PreferenceHelper>()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    fun regenerateDeviceIdentifier(): String {
        val identifier = Pairing.generateDeviceIdentifier()
        preferenceHelper.deviceIdentifier = identifier
        return identifier
    }

    fun sendDiscoverRequest() {
        if (!networkManager.isNetworkTrusted) {
            throw IllegalStateException("Network is not trusted")
        }

        val request = DiscoverRequest(discoverResponseListener.port)

        val packet = Pairing.encodeDiscoverRequest(request)
        packet.address = networkManager.broadcastAddress!!
        packet.port = Pairing.DEFAULT_DISCOVER_SERVER_PORT

        DatagramSocket().use {
            it.broadcast = true
            it.send(packet)
        }
    }

}
