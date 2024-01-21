package ru.sibwaf.inuyama.http

import io.javalin.http.Context
import io.javalin.json.fromJsonStream
import io.javalin.json.jsonMapper
import ru.sibwaf.inuyama.pairing.Session
import java.io.InputStream

class SecurityHttpFilter(private val config: SecurityConfig) : HttpFilter {

    companion object {
        private const val ATTRIBUTE_SESSION = "sibwaf.inuyama.session"
        private const val ATTRIBUTE_DECRYPTED_BODY_PROVIDER = "sibwaf.inuyama.decrypted-body-provider"
        private const val ATTRIBUTE_ENCRYPTABLE_RESPONSE_BODY = "sibwaf.inuyama.encryptable-response-body"

        var Context.session: Session?
            get() = attribute(ATTRIBUTE_SESSION)
            set(value) = attribute(ATTRIBUTE_SESSION, value)

        var Context.decryptedBodyProvider: () -> InputStream
            get() = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER) ?: { bodyInputStream() }
            set(value) = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER, value)

        var Context.encryptableResponseBody: InputStream?
            get() = attribute(ATTRIBUTE_ENCRYPTABLE_RESPONSE_BODY)
            set(value) = attribute(ATTRIBUTE_ENCRYPTABLE_RESPONSE_BODY, value)

        fun Context.requireSession(): Session = session ?: throw SessionException()

        fun Context.decryptedBody(): InputStream = decryptedBodyProvider()

        inline fun <reified T> Context.decryptBodyAs(): T {
            return jsonMapper().fromJsonStream(decryptedBody())
        }
    }

    override fun before(ctx: Context) {
        val strategy = config.getStrategy(ctx.path())
        if (!strategy.authenticate(ctx)) {
            throw SessionException()
        }
    }

    override fun after(ctx: Context) {
        config.getStrategy(ctx.path()).postProcess(ctx)
    }
}
