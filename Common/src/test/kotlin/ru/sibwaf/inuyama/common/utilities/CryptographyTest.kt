package ru.sibwaf.inuyama.common.utilities

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CryptographyTest {

    private class HumanReadableBytes(private val bytes: ByteArray) {

        private val readable by lazy { MD5.hash(bytes).takeLast(8) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HumanReadableBytes

            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }

        override fun toString() = readable
    }

    private val data by lazy {
        val text = CommonUtilities.generateRandomString(128, CommonUtilities.ALPHABET_ALNUM)
        return@lazy Encoding.stringToBytes(text)
    }

    private fun checkEntropy(provider: () -> ByteArray) {
        val variants = (1..10).map { provider() }.map { HumanReadableBytes(it) }

        expectThat(variants) {
            containsExactlyInAnyOrder(variants.distinct())
        }
    }

    private fun checkEncryptDecrypt(data: ByteArray, encrypt: (ByteArray) -> ByteArray, decrypt: (ByteArray) -> ByteArray) {
        val encrypted = encrypt(data)
        expectThat(encrypted) { isNotEqualTo(data) }

        val decrypted = decrypt(encrypted)
        expectThat(decrypted) { isEqualTo(data) }
    }

    @Test
    fun testRSA() {
        val keys = Cryptography.createRSAKeyPair()

        checkEncryptDecrypt(data, { Cryptography.encryptRSA(it, keys.public) }, { Cryptography.decryptRSA(it, keys.private) })
        checkEntropy { Cryptography.encryptRSA(data, keys.public) }
    }

    @Test
    fun testAES() {
        val key = Cryptography.createAESKey()

        checkEncryptDecrypt(data, { Cryptography.encryptAES(it, key) }, { Cryptography.decryptAES(it, key) })
        checkEntropy { Cryptography.encryptAES(data, key) }
    }

    @Test
    fun `Test AES encryption via InputStream`() {
        val key = Cryptography.createAESKey()

        val encrypted = Cryptography.encryptAES(ByteArrayInputStream(data), key).use {
            it.readAllBytes()
        }

        val decrypted = Cryptography.decryptAES(ByteArrayInputStream(encrypted), key).use {
            it.readAllBytes()
        }

        expect {
            that(encrypted)
                .describedAs("encrypted data")
                .isNotEqualTo(data)

            that(decrypted)
                .describedAs("decrypted data")
                .isEqualTo(data)
        }
    }

    @Test
    fun `Test AES encryption via OutputStream`() {
        val key = Cryptography.createAESKey()

        val encrypted = ByteArrayOutputStream()
        Cryptography.encryptAES(encrypted, key).use {
            it.write(data)
        }

        val decrypted = Cryptography.decryptAES(ByteArrayInputStream(encrypted.toByteArray()), key).use {
            it.readAllBytes()
        }

        expect {
            that(encrypted.toByteArray())
                .describedAs("encrypted data")
                .isNotEqualTo(data)

            that(decrypted)
                .describedAs("decrypted data")
                .isEqualTo(data)
        }
    }
}
