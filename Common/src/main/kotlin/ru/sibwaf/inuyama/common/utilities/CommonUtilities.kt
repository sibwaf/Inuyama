package ru.sibwaf.inuyama.common.utilities

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