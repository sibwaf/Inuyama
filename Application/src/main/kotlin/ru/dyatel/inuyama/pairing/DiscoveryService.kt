package ru.dyatel.inuyama.pairing

import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.DiscoverRequest
import ru.sibwaf.inuyama.common.Pairing
import sibwaf.inuyama.app.common.NetworkManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

private typealias Listener = (PairedServer) -> Unit

class DiscoveryService(
    private val networkManager: NetworkManager,
    private val preferenceHelper: PreferenceHelper,
) {

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

                val server = PairedServer(
                    host = packet.address.hostAddress ?: continue,
                    port = response.port,
                    key = response.key,
                    wasDiscovered = true,
                )

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