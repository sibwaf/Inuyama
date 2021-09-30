package ru.sibwaf.inuyama.http

import io.javalin.http.Context
import io.javalin.plugin.json.JavalinJson
import ru.sibwaf.inuyama.Session
import ru.sibwaf.inuyama.SessionException

class SecurityHttpFilter(private val config: SecurityConfig) : HttpFilter {

    companion object {
        private const val ATTRIBUTE_SESSION = "sibwaf.inuyama.session"
        private const val ATTRIBUTE_DECRYPTED_BODY_PROVIDER = "sibwaf.inuyama.decrypted-body-provider"

        var Context.session: Session?
            get() = attribute(ATTRIBUTE_SESSION)
            set(value) = attribute(ATTRIBUTE_SESSION, value)

        var Context.decryptedBodyProvider: () -> String
            get() = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER) ?: { body() }
            set(value) = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER, value)

        fun Context.requireSession(): Session = session ?: throw SessionException()

        fun Context.decryptBody(): String = decryptedBodyProvider()

        inline fun <reified T> Context.decryptBodyAs() = JavalinJson.fromJsonMapper.map(decryptBody(), T::class.java)
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
