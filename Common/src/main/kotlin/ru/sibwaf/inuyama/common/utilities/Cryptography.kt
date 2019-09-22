package ru.sibwaf.inuyama.common.utilities

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object Cryptography {

    private const val aesBlockSize = 16

    private val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
    private val aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

    private val random = SecureRandom()

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

    fun encryptAES(bytes: ByteArray, key: SecretKey): ByteArray {
        val iv = ByteArray(aesBlockSize)
        random.nextBytes(iv)

        val encryptedRaw = synchronized(aesCipher) {
            aesCipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            aesCipher.doFinal(bytes)
        }

        return iv + encryptedRaw
    }

    fun decryptAES(bytes: ByteArray, key: SecretKey): ByteArray {
        synchronized(aesCipher) {
            aesCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(bytes, 0, aesBlockSize))
            return aesCipher.doFinal(bytes, aesBlockSize, bytes.size - aesBlockSize)
        }
    }
}

private val humanReadableCache = mutableMapOf<PublicKey, String>()

val PublicKey.humanReadable: String
    get() = humanReadableCache.getOrPut(this) { MD5.hash(encoded).takeLast(8) }
