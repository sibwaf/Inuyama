package ru.sibwaf.inuyama.common.utilities

import java.math.BigInteger
import java.security.MessageDigest

object MD5 {

    fun hash(bytes: ByteArray): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(bytes)
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }

}
