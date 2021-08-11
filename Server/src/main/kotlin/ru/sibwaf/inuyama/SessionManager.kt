package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import javax.crypto.SecretKey

data class Session(val token: String, val key: SecretKey, val deviceId: String)

// todo: [critical] session timeout
// todo: [critical] session overwriting
class SessionManager {

    private companion object {
        const val TOKEN_LENGTH = 64
    }

    private val sessions = mutableMapOf<String, Session>()

    fun findSession(token: String): Session? = sessions[token]

    fun createSession(deviceId: String): Session {
        val token = CommonUtilities.generateRandomString(TOKEN_LENGTH, CommonUtilities.ALPHABET_ALNUM)

        return Session(token, Cryptography.createAESKey(), deviceId)
            .also { sessions[token] = it }
    }

}
