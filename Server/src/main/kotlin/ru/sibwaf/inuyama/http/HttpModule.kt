package ru.sibwaf.inuyama.http

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import org.kodein.di.Kodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.InuyamaConfiguration
import ru.sibwaf.inuyama.Module
import ru.sibwaf.inuyama.SessionException
import ru.sibwaf.inuyama.exception

val httpModule = Kodein.Module("http") {
    bind<SecurityHttpFilter>() with singleton {
        val insecurePaths = allInstances<HttpHandler>()
            .flatMap { it.insecurePaths }
            .toSet()

        SecurityHttpFilter(
            sessionManager = instance(),
            insecurePaths = insecurePaths
        )
    }

    bind<MainHttpHandler>() with singleton {
        MainHttpHandler(
            keyKeeper = instance(),
            sessionManager = instance()
        )
    }

    bind<HttpModule>() with singleton {
        HttpModule(
            config = instance(),
            gson = instance(),
            filters = allInstances(),
            handlers = allInstances()
        )
    }
}

private class HttpModule(
    private val config: InuyamaConfiguration,
    private val gson: Gson,

    private val filters: Collection<HttpFilter>,
    private val handlers: Collection<HttpHandler>
) : Module {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun install() {
        JavalinJson.fromJsonMapper = object : FromJsonMapper {
            override fun <T> map(json: String, targetClass: Class<T>) = gson.fromJson(json, targetClass)
        }
        JavalinJson.toJsonMapper = object : ToJsonMapper {
            override fun map(obj: Any) = gson.toJson(obj)
        }

        Javalin
            .create()
            .port(config.serverPort)
            .apply {
                // todo: filter ordering

                for (filter in filters) {
                    before(filter::before)
                }

                for (filter in filters.reversed()) {
                    after(filter::after)
                }

                exception<InterruptFilterChainException> { _, _ -> }

                for (handler in handlers) {
                    handler.install(this)
                }

                exception<SessionException> { _, ctx ->
                    ctx.status(401)
                }

                exception<Exception> { e, ctx ->
                    logger.error("Caught an unhandled exception", e)
                    ctx.status(500)
                }
            }
            .start()
    }
}
