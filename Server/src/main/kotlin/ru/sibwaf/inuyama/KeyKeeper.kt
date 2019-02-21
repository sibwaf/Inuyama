package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.io.File
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

// TODO: bad design, should be rewritten later (base64 in JSON config?)
class KeyKeeper {

    private companion object {
        val FILENAME = "server.key"
    }

    private var pair: KeyPair? = null
        get() {
            if (field == null) {
                try {
                    loadFromFile()
                } catch (e: Exception) {
                    regenerate()
                }
            }

            return field
        }

    val publicKey: PublicKey
        get() {
            return pair!!.public
        }

    val privateKey: PrivateKey
        get() {
            return pair!!.private
        }

    private fun saveToFile() {
        val bytes = Cryptography.encodeRSAKeyPair(pair!!)
        File(FILENAME).writeBytes(bytes)
    }

    private fun loadFromFile() {
        val bytes = File(FILENAME).readBytes()
        pair = Cryptography.decodeRSAKeyPair(bytes)
    }

    fun regenerate() {
        pair = Cryptography.createRSAKeyPair()
        saveToFile()
    }
}
