package ru.dyatel.inuyama.pairing

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.DiscoverRequest
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.PublicKey
import kotlin.concurrent.thread

private typealias Listener = (DiscoveredServer) -> Unit

data class DiscoveredServer(val address: InetAddress, val port: Int, val key: PublicKey)

class DiscoveryService(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()
    private val preferenceHelper by instance<PreferenceHelper>()

    private val listeners = mutableListOf<Listener>()

    val port: Int

    init {
        val socket = DatagramSocket()
        port = socket.localPort

        thread(isDaemon = true, name = "Discover response listener") {
            val buffer = Pairing.createDiscoverResponseBuffer()

            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)

                if (!networkManager.isNetworkTrusted) {
                    continue
                }

                val listeners = synchronized(listeners) { listeners.toList() }
                        .takeIf { it.isNotEmpty() }
                        ?: continue

                val response = Pairing.decodeDiscoverResponse(packet) ?: continue

                val server = DiscoveredServer(packet.address, response.port, response.key)
                for (listener in listeners) {
                    listener(server)
                }
            }
        }
    }

    fun addListener(listener: Listener): Listener {
        synchronized(listeners) { listeners.add(listener) }
        return listener
    }

    fun removeListener(listener: Listener) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    fun sendDiscoverRequest() {
        check(networkManager.isNetworkTrusted) { "Network is not trusted" }

        val request = DiscoverRequest(port)

        val packet = Pairing.encodeDiscoverRequest(request)
        packet.address = networkManager.broadcastAddress!!
        packet.port = preferenceHelper.discoveryPort

        DatagramSocket().use {
            it.broadcast = true
            it.send(packet)
        }
    }

}