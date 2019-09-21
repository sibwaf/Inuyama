package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.security.SecureRandom
import javax.crypto.SecretKey
import kotlin.streams.toList

data class Session(val token: String, val key: SecretKey)

class SessionManager {

    private companion object {
        const val TOKEN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val TOKEN_LENGTH = 64
    }

    private val random = SecureRandom()

    private val sessions = mutableMapOf<String, Session>()

    fun findSession(token: String): Session? = sessions[token]

    fun createSession(): Session {
        val token = random.ints(TOKEN_LENGTH.toLong(), 0, TOKEN_ALPHABET.length)
                .toList()
                .map { TOKEN_ALPHABET[it] }
                .joinToString("")

        val session = Session(token, Cryptography.createAESKey())
        return session.also { sessions[token] = it }
    }

}
