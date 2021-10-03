package ru.sibwaf.inuyama.pairing

import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.InuyamaConfiguration
import ru.sibwaf.inuyama.KeyKeeper
import ru.sibwaf.inuyama.common.DiscoverResponse
import ru.sibwaf.inuyama.common.Pairing
import ru.sibwaf.inuyama.common.utilities.humanReadable
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

class PairingManager(override val kodein: Kodein) : KodeinAware {

    private companion object {
        val logger = LoggerFactory.getLogger(PairingManager::class.java)!!
    }

    private val keyKeeper by instance<KeyKeeper>()
    private val configuration by instance<InuyamaConfiguration>()

    fun start() {
        thread(isDaemon = true, name = "Discover request listener") {
            val socket = DatagramSocket(configuration.discoveryPort)
            val requestBuffer = Pairing.createDiscoverRequestBuffer()

            logger.info("Listening for discover requests @ port ${configuration.discoveryPort} w/ key ${keyKeeper.keyPair.public.humanReadable}")

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
