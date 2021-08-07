package ru.sibwaf.inuyama.web

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

val webModule = Kodein.Module("web") {
    bind<SecurityFilter>() with singleton {
        val insecurePaths = allInstances<WebHandler>()
            .flatMap { it.insecurePaths }
            .toSet()

        SecurityFilter(
            sessionManager = instance(),
            insecurePaths = insecurePaths
        )
    }

    bind<MainWebHandler>() with singleton {
        MainWebHandler(
            keyKeeper = instance(),
            sessionManager = instance()
        )
    }

    bind<WebModule>() with singleton {
        WebModule(
            config = instance(),
            gson = instance(),
            filters = allInstances(),
            handlers = allInstances()
        )
    }
}

private class WebModule(
    private val config: InuyamaConfiguration,
    private val gson: Gson,

    private val filters: Collection<WebFilter>,
    private val handlers: Collection<WebHandler>
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
