package ru.sibwaf.inuyama.common

import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.readInt
import ru.sibwaf.inuyama.common.utilities.writeInt
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.security.PublicKey
import java.util.Arrays
import kotlin.random.Random

class DiscoverRequest(val port: Int)

class DiscoverResponse(val port: Int, val key: PublicKey)

object Pairing {

    const val DEFAULT_DISCOVER_SERVER_PORT = 50505

    private val CHARSET = Charsets.US_ASCII

    private val DISCOVER_PREFIX = """
Ij6qWiSzUARrUQDUrs9VoRiA0BaugPMKblqn2cGW2mHoUYVWIupx5yhjVnrS04QK
GXdp2VmUzWy86QG46aKjAksk99BeJrvaf64jWLjRSmapsNydz4pw8pjbYBk4Wv6B
mr6GBYC0XyN2xxg046n3nEePumNFYSyHIAubalVvy6Nk9usl9gx2VLjOyZ9WW6Sp
mEpkLHxO2BxXRg88OkWuBtMRCXbL3jZFwXlyEv0zHobRFr5xWH1GZkaYwDjh4aRS
""".replace("\n", "").toByteArray(CHARSET)

    private val DISCOVER_PREFIX_LENGTH = DISCOVER_PREFIX.size
    private val DISCOVER_RESPONSE_MAX_KEY_LENGTH = 512

    private val DISCOVER_REQUEST_LENGTH = DISCOVER_PREFIX_LENGTH + 2 * 4
    private val DISCOVER_RESPONSE_LENGTH = DISCOVER_PREFIX_LENGTH + 2 * 4 + DISCOVER_RESPONSE_MAX_KEY_LENGTH

    private const val DEVICE_IDENTIFIER_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val DEVICE_IDENTIFIER_LENGTH = 32

    fun generateDeviceIdentifier(): String {
        val builder = StringBuilder(DEVICE_IDENTIFIER_LENGTH)

        for (i in 0 until DEVICE_IDENTIFIER_LENGTH) {
            val index = Random.nextInt(DEVICE_IDENTIFIER_ALPHABET.length)
            val character = DEVICE_IDENTIFIER_ALPHABET[index]
            builder.append(character)
        }

        return builder.toString()
    }

    private fun ByteArrayOutputStream.writePrefix() = write(DISCOVER_PREFIX)

    private fun ByteArrayInputStream.checkPrefix(): Boolean {
        val buffer = ByteArray(DISCOVER_PREFIX_LENGTH)
        read(buffer, 0, buffer.size)
        return Arrays.equals(DISCOVER_PREFIX, buffer)
    }

    private fun ByteArrayOutputStream.writePublicKey(key: PublicKey) {
        val encoded = Cryptography.encodeRSAPublicKey(key)
        if (encoded.size > DISCOVER_RESPONSE_MAX_KEY_LENGTH) {
            throw IllegalArgumentException("Key is too long: ${encoded.size}")
        }

        writeInt(encoded.size)
        write(encoded)
    }

    private fun ByteArrayInputStream.readPublicKey(): PublicKey {
        val length = readInt()

        val buffer = ByteArray(length)
        read(buffer, 0, buffer.size)
        return Cryptography.decodeRSAPublicKey(buffer)
    }

    fun createDiscoverRequestBuffer() = ByteArray(DISCOVER_REQUEST_LENGTH)

    fun createDiscoverResponseBuffer() = ByteArray(DISCOVER_RESPONSE_LENGTH)

    fun encodeDiscoverRequest(request: DiscoverRequest): DatagramPacket {
        val stream = ByteArrayOutputStream(DISCOVER_REQUEST_LENGTH)
        stream.writePrefix()
        stream.writeInt(request.port)

        val buffer = stream.toByteArray()
        return DatagramPacket(buffer, buffer.size)
    }

    fun decodeDiscoverRequest(packet: DatagramPacket): DiscoverRequest? {
        if (packet.data.size != DISCOVER_REQUEST_LENGTH) {
            return null
        }

        val stream = ByteArrayInputStream(packet.data)
        if (!stream.checkPrefix()) {
            return null
        }

        val port = stream.readInt()

        return DiscoverRequest(port)
    }

    fun encodeDiscoverResponse(response: DiscoverResponse): DatagramPacket {
        val stream = ByteArrayOutputStream(DISCOVER_RESPONSE_LENGTH)
        stream.writePrefix()
        stream.writeInt(response.port)
        stream.writePublicKey(response.key)

        val buffer = stream.toByteArray()
        return DatagramPacket(buffer, buffer.size)
    }

    fun decodeDiscoverResponse(packet: DatagramPacket): DiscoverResponse? {
        if (packet.data.size != DISCOVER_RESPONSE_LENGTH) {
            return null
        }

        val stream = ByteArrayInputStream(packet.data)
        if (!stream.checkPrefix()) {
            return null
        }

        val port = stream.readInt()
        val key = stream.readPublicKey()

        return DiscoverResponse(port, key)
    }

}
