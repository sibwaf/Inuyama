package ru.sibwaf.inuyama.common.utilities

import com.google.common.io.BaseEncoding
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

object Cryptography {

    fun createRSAKeyPair(length: Int = 2048): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(length)
        return generator.genKeyPair()
    }

    fun encodeRSAKeyPair(keyPair: KeyPair): ByteArray {
        ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { objectStream ->
                objectStream.writeObject(keyPair)
            }

            return byteStream.toByteArray()
        }
    }

    fun decodeRSAKeyPair(encoded: ByteArray): KeyPair {
        ByteArrayInputStream(encoded).use { byteStream ->
            ObjectInputStream(byteStream).use { objectStream ->
                return objectStream.readObject() as KeyPair
            }
        }
    }

    fun encodeRSAPublicKey(key: PublicKey): ByteArray {
        return key.encoded
    }

    fun decodeRSAPublicKey(encoded: ByteArray): PublicKey {
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded))
    }

    fun encodeRSAPublicKeyBase64(key: PublicKey): String {
        val bytes = encodeRSAPublicKey(key)
        return BaseEncoding.base64().encode(bytes)
    }

    fun decodeRSAPublicKeyBase64(encoded: String): PublicKey {
        val bytes = BaseEncoding.base64().decode(encoded)
        return decodeRSAPublicKey(bytes)
    }

}

private val humanReadableCache = mutableMapOf<PublicKey, String>()

val PublicKey.humanReadable: String
    get() = humanReadableCache.getOrPut(this) { MD5.hash(encoded).takeLast(8) }
