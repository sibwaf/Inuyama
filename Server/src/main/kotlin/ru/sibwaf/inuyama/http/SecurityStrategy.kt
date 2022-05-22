package ru.sibwaf.inuyama.http

import io.javalin.http.Context
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptedBodyProvider
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.session
import ru.sibwaf.inuyama.pairing.PairedSessionManager
import java.net.InetAddress

fun interface SecurityStrategy {

    object AllowAll : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean = true
    }

    object DenyAll : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean = false
    }

    class BasicAuth(private val authenticator: HttpAuthenticator) : SecurityStrategy {

        private companion object {
            const val ATTRIBUTE_BASIC_AUTH_SUCCEEDED = "sibwaf.inuyama.basic-auth-succeeded"
        }

        override fun authenticate(ctx: Context): Boolean {
            val credentials = ctx.header("Authorization")
                ?.takeIf { it.startsWith("Basic ") }
                ?.removePrefix("Basic ")
                ?.let { Encoding.decodeBase64(it) }
                ?.let { Encoding.bytesToString(it) }
                ?.takeIf { ":" in it }

            val result = if (credentials != null) {
                val username = credentials.substringBefore(":")
                val password = credentials.substringAfter(":")

                authenticator.checkCredentials(username, password)
            } else {
                false
            }

            ctx.attribute(ATTRIBUTE_BASIC_AUTH_SUCCEEDED, result)
            return result
        }

        override fun postProcess(ctx: Context) {
            if (ctx.attribute<Boolean>(ATTRIBUTE_BASIC_AUTH_SUCCEEDED) != true) {
                ctx.header("WWW-Authenticate", "Basic realm=\"master\", charset=\"UTF-8\"")
            }
        }
    }

    class PairedAuth(private val pairedSessionManager: PairedSessionManager) : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean {
            val session = ctx.header("Authorization")
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
                ?.let { pairedSessionManager.findSession(it) }
                ?: return false

            ctx.session = session

            ctx.decryptedBodyProvider = {
                Cryptography.decryptAES(ctx.bodyAsInputStream(), session.key)
            }

            return true
        }

        override fun postProcess(ctx: Context) {
            val response = ctx.resultStream() ?: return

            response
                .let { Cryptography.encryptAES(it, ctx.requireSession().key) }
                .let { ctx.result(it) }

            ctx.contentType("application/octet-stream")
        }
    }

    class AddressWhitelist(private val whitelist: Set<InetAddress>) : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean {
            val sender = InetAddress.getByName(ctx.req.remoteAddr)
            return sender in whitelist
        }
    }

    fun authenticate(ctx: Context): Boolean

    fun postProcess(ctx: Context) {
    }
}
