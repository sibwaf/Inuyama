package ru.sibwaf.inuyama

import java.security.SecureRandom
import kotlin.streams.toList

// TODO: key, TTL
class Session

class SessionManager {

    private companion object {
        const val SESSION_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        const val SESSION_ID_LENGTH = 64
    }

    private val random = SecureRandom()

    private val sessions = mutableMapOf<String, Session>()

    fun findSession(id: String): Session? = sessions[id]

    fun createSession(): String {
        val id = random.ints(SESSION_ID_LENGTH.toLong(), 0, SESSION_ID_ALPHABET.length)
                .toList()
                .map { SESSION_ID_ALPHABET[it] }
                .toCharArray()
                .let { String(it) }

        sessions[id] = Session()

        return id
    }

}
