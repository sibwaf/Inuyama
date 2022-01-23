package ru.sibwaf.inuyama.http

import ru.sibwaf.inuyama.common.utilities.SHA256
import java.nio.charset.StandardCharsets

interface HttpAuthenticator {
    fun checkCredentials(username: String, password: String): Boolean
}

// todo: customizable hash functions
class InMemoryHttpAuthenticator(credentials: Map<String, String>) : HttpAuthenticator {

    private companion object {
        val ENCODING = StandardCharsets.UTF_8
    }

    constructor(vararg credentials: Pair<String, String>) : this(credentials.toMap())

    private val credentials = credentials.mapValues { (_, password) -> hashPassword(password) }

    override fun checkCredentials(username: String, password: String): Boolean {
        val expectedPasswordHash = credentials[username] ?: return false
        return expectedPasswordHash == hashPassword(password)
    }

    private fun hashPassword(password: String): String {
        val buffer = ENCODING.encode(password)
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return SHA256.hash(bytes)
    }
}
