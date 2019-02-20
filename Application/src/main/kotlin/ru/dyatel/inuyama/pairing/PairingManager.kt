package ru.dyatel.inuyama.pairing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.DiscoverRequest
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramSocket
import java.security.PublicKey

data class PairedServer(val key: PublicKey)

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()
    private val discoverResponseListener by instance<DiscoverResponseListener>()

    private val preferenceHelper by instance<PreferenceHelper>()

    val deviceIdentifier: String
        get() = preferenceHelper.deviceIdentifier ?: regenerateDeviceIdentifier()

    var pairedServer: PairedServer?
        get() = preferenceHelper.pairedServer
        set(value) {
            preferenceHelper.pairedServer = value
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
        packet.port = Pairing.DEFAULT_DISCOVER_SERVER_PORT

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

        lateinit var waiter: Job

        val listener: (DiscoveredServer) -> Unit = { server ->
            discovered = server.takeIf { equalsToPaired(it) }

            if (discovered != null) {
                runBlocking { waiter.cancelAndJoin() }
            }
        }

        waiter = GlobalScope.launch(Dispatchers.Default) {
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
