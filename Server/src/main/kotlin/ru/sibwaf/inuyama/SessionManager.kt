package ru.sibwaf.inuyama

import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

data class Session(val token: String, val key: SecretKey, val deviceId: String)

// todo: [critical] session timeout
class SessionManager {

    private companion object {
        const val TOKEN_LENGTH = 64
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    fun findSession(token: String): Session? = sessions[token]

    fun createSession(deviceId: String): Session {
        val key = Cryptography.createAESKey()

        var token: String
        var session: Session
        while (true) {
            token = CommonUtilities.generateRandomString(TOKEN_LENGTH, CommonUtilities.ALPHABET_ALNUM)
            session = Session(token, key, deviceId)
            if (sessions.putIfAbsent(token, session) == null) {
                break
            }
        }

        return session
    }
}
