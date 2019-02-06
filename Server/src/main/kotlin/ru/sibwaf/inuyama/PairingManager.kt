package ru.sibwaf.inuyama

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.DiscoverResponse
import ru.sibwaf.inuyama.common.Pairing
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private companion object {
        val publicKey by lazy {
            val keyPair = Cryptography.createRSAKeyPair(2048)
            return@lazy keyPair.public!!
        }
    }

    init {
        thread(isDaemon = true, name = "Discover request listener") {
            val socket = DatagramSocket(Pairing.DEFAULT_DISCOVER_SERVER_PORT)
            val requestBuffer = Pairing.createDiscoverRequestBuffer()

            while (true) {
                val requestPacket = DatagramPacket(requestBuffer, requestBuffer.size)
                socket.receive(requestPacket)

                val request = Pairing.decodeDiscoverRequest(requestPacket) ?: continue

                val response = DiscoverResponse(0, publicKey) // TODO: port

                val responsePacket = Pairing.encodeDiscoverResponse(response)
                responsePacket.address = requestPacket.address
                responsePacket.port = request.port

                socket.send(responsePacket)
            }
        }
    }

}
