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

    private companion object {
        val DEVICE_IDENTIFIER_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val DEVICE_IDENTIFIER_LENGTH = 32
    }

    private val networkManager by instance<NetworkManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()

    private val preferenceHelper by instance<PreferenceHelper>()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    fun regenerateDeviceIdentifier(): String {
        val builder = StringBuilder(DEVICE_IDENTIFIER_LENGTH)

        for (i in 0 until DEVICE_IDENTIFIER_LENGTH) {
            val index = Random.nextInt(DEVICE_IDENTIFIER_ALPHABET.length)
            val character = DEVICE_IDENTIFIER_ALPHABET[index]
            builder.append(character)
        }

        val identifier = builder.toString()
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
