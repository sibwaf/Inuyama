package ru.sibwaf.inuyama.common.utilities

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object Cryptography {

    private const val aesBlockSize = 16

    private val rsaCipher = makeRsaCipher()
    private val aesCipher = makeAesCipher()

    private val random = SecureRandom()

    private fun makeRsaCipher(): Cipher {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
    }

    private fun makeAesCipher(): Cipher {
        return Cipher.getInstance("AES/CBC/PKCS5Padding")
    }

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

    fun encryptAES(stream: InputStream, key: SecretKey): InputStream {
        val iv = ByteArray(aesBlockSize)
        random.nextBytes(iv)

        val cipher = makeAesCipher().apply {
            init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        }

        return ChainedInputStream(ByteArrayInputStream(iv), CipherInputStream(stream, cipher))
    }

    fun encryptAES(stream: OutputStream, key: SecretKey): OutputStream {
        val iv = ByteArray(aesBlockSize)
        random.nextBytes(iv)

        val cipher = makeAesCipher().apply {
            init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        }

        stream.write(iv)
        return CipherOutputStream(stream, cipher)
    }

    fun decryptAES(bytes: ByteArray, key: SecretKey): ByteArray {
        synchronized(aesCipher) {
            aesCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(bytes, 0, aesBlockSize))
            return aesCipher.doFinal(bytes, aesBlockSize, bytes.size - aesBlockSize)
        }
    }

    fun decryptAES(stream: InputStream, key: SecretKey): InputStream {
        val iv = ByteArray(aesBlockSize)
        stream.read(iv, 0, aesBlockSize)

        val cipher = makeAesCipher().apply {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv, 0, aesBlockSize))
        }

        return CipherInputStream(stream, cipher)
    }
}

val PublicKey.humanReadable: String
    get() = MD5.hash(encoded).takeLast(8)
