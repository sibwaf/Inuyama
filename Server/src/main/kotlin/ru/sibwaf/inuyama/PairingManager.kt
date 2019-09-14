package ru.sibwaf.inuyama

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.sibwaf.inuyama.common.DiscoverResponse
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private val keyKeeper by instance<KeyKeeper>()
    private val configuration by instance<InuyamaConfiguration>()

    fun start() {
        thread(isDaemon = true, name = "Discover request listener") {
            val socket = DatagramSocket(configuration.discoveryPort)
            val requestBuffer = Pairing.createDiscoverRequestBuffer()

            while (true) {
                val requestPacket = DatagramPacket(requestBuffer, requestBuffer.size)
                socket.receive(requestPacket)

                val request = Pairing.decodeDiscoverRequest(requestPacket) ?: continue

                val response = DiscoverResponse(configuration.serverPort, keyKeeper.keyPair.public)

                val responsePacket = Pairing.encodeDiscoverResponse(response)
                responsePacket.address = requestPacket.address
                responsePacket.port = request.port

                socket.send(responsePacket)
            }
        }
    }

}
