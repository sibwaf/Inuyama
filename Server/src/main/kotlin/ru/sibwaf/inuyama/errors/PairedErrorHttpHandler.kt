package ru.sibwaf.inuyama.errors

import io.javalin.Javalin
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.common.paired.model.ErrorLogEntry
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBodyAs
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.requireSession
import ru.sibwaf.inuyama.http.pairedSubroute
import ru.sibwaf.inuyama.http.subroute

class PairedErrorHttpHandler : HttpHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun Javalin.install() {
        pairedSubroute {
            subroute("/errors") {
                post("/") { ctx ->
                    val session = ctx.requireSession()
                    val event = ctx.decryptBodyAs<ErrorLogEntry>()
                    log.error("Device ${session.deviceId} had a error at ${event.timestamp}: ${event.stacktrace}")
                }
            }
        }
    }
}
