package ru.sibwaf.inuyama.common.utilities

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object Cryptography {

    private val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")

    fun createRSAKeyPair(length: Int = 2048): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(length)
        return generator.genKeyPair()
    }

    fun encryptRSA(bytes: ByteArray, key: PublicKey): ByteArray {
        synchronized(rsaCipher) {
            rsaCipher.init(Cipher.ENCRYPT_MODE, key)
            return rsaCipher.doFinal(bytes)
        }
    }

    fun decryptRSA(bytes: ByteArray, key: PrivateKey): ByteArray {
        synchronized(rsaCipher) {
            rsaCipher.init(Cipher.DECRYPT_MODE, key)
            return rsaCipher.doFinal(bytes)
        }
    }

    fun createAESKey(length: Int = 256): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(length)
        return generator.generateKey()
    }
}

private val humanReadableCache = mutableMapOf<PublicKey, String>()

val PublicKey.humanReadable: String
    get() = humanReadableCache.getOrPut(this) { MD5.hash(encoded).takeLast(8) }
