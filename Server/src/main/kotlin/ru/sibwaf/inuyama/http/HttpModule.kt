package ru.sibwaf.inuyama.http

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.json.FromJsonMapper
import io.javalin.plugin.json.JavalinJson
import io.javalin.plugin.json.ToJsonMapper
import org.kodein.di.Kodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.InuyamaConfiguration
import ru.sibwaf.inuyama.Module
import ru.sibwaf.inuyama.exception
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Paths

private const val SUBROUTE_PATH_PAIRED = "/paired"
private const val SUBROUTE_PATH_WEB = "/web"

val httpModule = Kodein.Module("http") {
    bind<SecurityHttpFilter>() with singleton {
        val inuyamaConfiguration = instance<InuyamaConfiguration>()

        val mainStrategy = if (inuyamaConfiguration.webAuth != null) {
            val (username, password) = inuyamaConfiguration.webAuth
            SecurityStrategy.BasicAuth(
                InMemoryHttpAuthenticator(username to password)
            )
        } else {
            SecurityStrategy.AddressWhitelist(
                InetAddress.getAllByName("localhost").toSet()
            )
        }

        val config = securityConfig(mainStrategy) {
            subroute(SUBROUTE_PATH_PAIRED) {
                strategy = SecurityStrategy.PairedAuth(instance())

                subroute("/ping") { strategy = SecurityStrategy.AllowAll }
                subroute("/bind-session") { strategy = SecurityStrategy.AllowAll }
            }
        }

        SecurityHttpFilter(config)
    }

    bind<MainHttpHandler>() with singleton {
        MainHttpHandler(
            keyKeeper = instance(),
            pairedSessionManager = instance()
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
            .create {
                val path = "frontend"
                if (Files.isDirectory(Paths.get(path))) {
                    it.addStaticFiles("frontend", Location.EXTERNAL)
                } else {
                    logger.warn("[$path] not found, static resources won't be served")
                }
            }
            .start(config.serverPort)
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
                    with(handler) {
                        install()
                    }
                }

                exception<SessionException> { _, ctx ->
                    ctx.status(401)
                }

                exception<Exception> { e, ctx ->
                    logger.error("Caught an unhandled exception", e)
                    ctx.status(500)
                }
            }
    }
}

fun Javalin.pairedSubroute(block: HttpSubroute.() -> Unit) = subroute(SUBROUTE_PATH_PAIRED, block)

fun Javalin.webSubroute(block: HttpSubroute.() -> Unit) = subroute(SUBROUTE_PATH_WEB, block)
