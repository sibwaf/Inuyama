package ru.sibwaf.inuyama.web

import io.javalin.Context
import io.javalin.json.JavalinJson
import ru.sibwaf.inuyama.Session
import ru.sibwaf.inuyama.SessionException
import ru.sibwaf.inuyama.SessionManager
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding

class SecurityFilter(
    private val sessionManager: SessionManager,
    private val insecurePaths: Set<String>
) : WebFilter {

    companion object {
        const val ATTRIBUTE_SESSION = "sibwaf.inuyama.session"
        private const val ATTRIBUTE_DECRYPTED_BODY = "sibwaf.inuyama.request-body"

        fun Context.decryptedBody(): String = attribute<() -> String>(ATTRIBUTE_DECRYPTED_BODY)!!.invoke()

        inline fun <reified T> Context.decryptBodyAs() = JavalinJson.fromJsonMapper.map(decryptedBody(), T::class.java)
    }

    override fun before(ctx: Context) {
        if (!isSecured(ctx.path())) {
            ctx.attribute(ATTRIBUTE_DECRYPTED_BODY, { ctx.body() })
            return
        }

        val session = ctx.header("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.let { sessionManager.findSession(it) }
            ?: throw SessionException()

        ctx.attribute(ATTRIBUTE_SESSION, session)

        val bodyDecryptor: () -> String = {
            ctx.body()
                .let { Encoding.decodeBase64(it) }
                .let { Cryptography.decryptAES(it, session.key) }
                .let { Encoding.bytesToString(it) }
        }

        ctx.attribute(ATTRIBUTE_DECRYPTED_BODY, bodyDecryptor)
    }

    override fun after(ctx: Context) {
        val session = ctx.attribute<Session>(ATTRIBUTE_SESSION) ?: return

        val response = ctx.resultString() ?: return
        response
            .let { Encoding.stringToBytes(it) }
            .let { Cryptography.encryptAES(it, session.key) }
            .let { Encoding.encodeBase64(it) }
            .let { ctx.result(it) }

        ctx.contentType("text/plain")
    }

    private fun isSecured(path: String): Boolean = insecurePaths.none { path.startsWith(it) }
}
