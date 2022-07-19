package ru.sibwaf.inuyama.common.utilities

import java.io.FilterInputStream
import java.io.InputStream
import java.security.SecureRandom

object CommonUtilities {

    const val ALPHABET_ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    private val random = SecureRandom()

    fun generateRandomString(length: Int, alphabet: String): String {
        val result = StringBuilder(length)
        for (i in 0 until length) {
            val index = random.nextInt(alphabet.length)
            result.append(alphabet[index])
        }
        return result.toString()
    }
}

fun InputStream.nonCloseable(): InputStream {
    return object : FilterInputStream(this) {
        override fun close() = Unit
    }
}

inline fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}
