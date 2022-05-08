package ru.sibwaf.inuyama.http

import io.javalin.http.Context
import io.javalin.plugin.json.JavalinJson
import ru.sibwaf.inuyama.Session
import ru.sibwaf.inuyama.SessionException
import ru.sibwaf.inuyama.common.utilities.Encoding
import java.io.InputStream

class SecurityHttpFilter(private val config: SecurityConfig) : HttpFilter {

    companion object {
        private const val ATTRIBUTE_SESSION = "sibwaf.inuyama.session"
        private const val ATTRIBUTE_DECRYPTED_BODY_PROVIDER = "sibwaf.inuyama.decrypted-body-provider"

        var Context.session: Session?
            get() = attribute(ATTRIBUTE_SESSION)
            set(value) = attribute(ATTRIBUTE_SESSION, value)

        var Context.decryptedBodyProvider: () -> InputStream
            get() = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER) ?: { bodyAsInputStream() }
            set(value) = attribute(ATTRIBUTE_DECRYPTED_BODY_PROVIDER, value)

        fun Context.requireSession(): Session = session ?: throw SessionException()

        fun Context.decryptedBody(): InputStream = decryptedBodyProvider()

        inline fun <reified T> Context.decryptBodyAs(): T {
            val bytes = decryptedBody().use { it.readAllBytes() }
            val body = Encoding.bytesToString(bytes)
            return JavalinJson.fromJsonMapper.map(body, T::class.java)
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
