package ru.sibwaf.inuyama.common.utilities

import com.google.common.io.BaseEncoding
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object Encoding {

    val CHARSET = StandardCharsets.UTF_8

    fun encodeBase64(bytes: ByteArray): String = BaseEncoding.base64().encode(bytes)
    fun decodeBase64(encoded: String): ByteArray = BaseEncoding.base64().decode(encoded)

    fun stringToBytes(text: String): ByteArray = text.toByteArray(CHARSET)
    fun bytesToString(bytes: ByteArray): String = String(bytes, CHARSET)

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

    fun encodeAESKey(key: SecretKey): ByteArray {
        return key.encoded
    }

    fun decodeAESKey(encoded: ByteArray): SecretKey {
        return SecretKeySpec(encoded, "AES")
    }
}