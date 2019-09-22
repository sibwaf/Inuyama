package ru.dyatel.inuyama.pairing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.DiscoverRequest
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.net.DatagramSocket
import java.security.KeyPair
import java.security.PublicKey

data class PairedServer(val key: PublicKey)

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()

    private val preferenceHelper by instance<PreferenceHelper>()

    val deviceKeyPair: KeyPair
        get() = preferenceHelper.keyPair ?: regenerateDeviceKeyPair()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    var pairedServer: PairedServer?
        get() = preferenceHelper.pairedServer
        set(value) {
            preferenceHelper.pairedServer = value
        }

    fun regenerateDeviceKeyPair(): KeyPair {
        val keyPair = Cryptography.createRSAKeyPair()
        preferenceHelper.keyPair = keyPair
        return keyPair
    }

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
        packet.port = preferenceHelper.discoveryPort

        DatagramSocket().use {
            it.broadcast = true
            it.send(packet)
        }
    }

    fun equalsToPaired(server: DiscoveredServer): Boolean {
        return server.key == pairedServer?.key
    }

    fun findPairedServer(): DiscoveredServer? {
        if (pairedServer == null) {
            return null
        }

        var discovered: DiscoveredServer? = null

        val lock = Any()
        lateinit var waiter: Job

        val listener: (DiscoveredServer) -> Unit = listener@{ server ->
            if (discovered != null || !equalsToPaired(server)) {
                return@listener
            }

            synchronized(lock) {
                if (discovered == null) {
                    discovered = server
                    waiter.cancel()
                }
            }
        }

        waiter = GlobalScope.async(Dispatchers.Default) {
            discoverResponseListener.addListener(listener)
            sendDiscoverRequest()
            delay(10000) // TODO: magic constants
        }
        waiter.invokeOnCompletion {
            discoverResponseListener.removeListener(listener)
        }

        runBlocking { waiter.join() }
        return discovered
    }

    fun unbind() {
        pairedServer = null
    }

    fun bind(server: DiscoveredServer) {
        pairedServer = PairedServer(server.key)
    }

}
