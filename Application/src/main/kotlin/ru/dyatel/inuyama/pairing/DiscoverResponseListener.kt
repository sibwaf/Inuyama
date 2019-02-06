package ru.dyatel.inuyama.pairing

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.sibwaf.inuyama.common.DiscoverResponse
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

private typealias Listener = (DiscoverResponse, InetAddress) -> Unit

class DiscoverResponseListener(override val kodein: Kodein) : KodeinAware {

    private val networkManager by instance<NetworkManager>()

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

                for (listener in listeners) {
                    listener(response, packet.address)
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

}