package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import javax.crypto.SecretKey

data class Session(val token: String, val key: SecretKey)

class SessionManager {

    private companion object {
        const val TOKEN_LENGTH = 64
    }

    private val sessions = mutableMapOf<String, Session>()

    fun findSession(token: String): Session? = sessions[token]

    fun createSession(): Session {
        val token = CommonUtilities.generateRandomString(TOKEN_LENGTH, CommonUtilities.ALPHABET_ALNUM)

        val session = Session(token, Cryptography.createAESKey())
        return session.also { sessions[token] = it }
    }

}
