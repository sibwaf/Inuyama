package ru.sibwaf.inuyama.pairing

import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
import kotlin.concurrent.thread

data class Session(
    val token: String,
    val key: SecretKey,
    val deviceId: String,
    val createdAt: OffsetDateTime,
)

class PairedSessionManager {

    private companion object {
        const val TOKEN_LENGTH = 64

        val SESSION_EXPIRATION_PERIOD = Duration.ofSeconds(10)
        val SESSION_TOKEN_LEASE_PERIOD = Duration.ofDays(2)
    }

    private val sessions = ConcurrentHashMap<String, Session>()

    init {
        thread(isDaemon = true, name = "Session cleaner") {
            while (true) {
                val now = OffsetDateTime.now()
                for (key in sessions.keys()) {
                    val session = sessions[key] ?: continue

                    val expiresAt = session.createdAt + SESSION_EXPIRATION_PERIOD + SESSION_TOKEN_LEASE_PERIOD
                    if (expiresAt < now) {
                        sessions.remove(key)
                    }
                }

                try {
                    Thread.sleep(60 * 1000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    fun findSession(token: String): Session? {
        return sessions[token]
            ?.takeIf { it.createdAt + SESSION_EXPIRATION_PERIOD > OffsetDateTime.now() }
    }

    fun createSession(deviceId: String): Session {
        val key = Cryptography.createAESKey()
        val createdAt = OffsetDateTime.now()

        var token: String
        var session: Session
        while (true) {
            token = CommonUtilities.generateRandomString(TOKEN_LENGTH, CommonUtilities.ALPHABET_ALNUM)
            session = Session(
                token = token,
                key = key,
                deviceId = deviceId,
                createdAt = createdAt,
            )

            if (sessions.putIfAbsent(token, session) == null) {
                break
            }
        }

        return session
    }
}
