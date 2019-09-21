package ru.sibwaf.inuyama.common.utilities

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey

object Cryptography {

    fun createRSAKeyPair(length: Int = 2048): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(length)
        return generator.genKeyPair()
    }
}

private val humanReadableCache = mutableMapOf<PublicKey, String>()

val PublicKey.humanReadable: String
    get() = humanReadableCache.getOrPut(this) { MD5.hash(encoded).takeLast(8) }
