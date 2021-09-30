package ru.sibwaf.inuyama.http

import io.javalin.http.Context
import ru.sibwaf.inuyama.SessionManager
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptedBodyProvider
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.session

fun interface SecurityStrategy {

    object AllowAll : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean = true
    }

    object DenyAll : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean = false
    }

    class PairedAuth(private val sessionManager: SessionManager) : SecurityStrategy {
        override fun authenticate(ctx: Context): Boolean {
            val session = ctx.header("Authorization")
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")
                ?.let { sessionManager.findSession(it) }
                ?: return false

            ctx.session = session

            ctx.decryptedBodyProvider = {
                ctx.body()
                    .let { Encoding.decodeBase64(it) }
                    .let { Cryptography.decryptAES(it, session.key) }
                    .let { Encoding.bytesToString(it) }
            }

            return true
        }

        override fun postProcess(ctx: Context) {
            val response = ctx.resultString() ?: return
            response
                .let { Encoding.stringToBytes(it) }
                .let { Cryptography.encryptAES(it, ctx.requireSession().key) }
                .let { Encoding.encodeBase64(it) }
                .let { ctx.result(it) }

            ctx.contentType("text/plain")
        }
    }

    fun authenticate(ctx: Context): Boolean

    fun postProcess(ctx: Context) {
    }
}
