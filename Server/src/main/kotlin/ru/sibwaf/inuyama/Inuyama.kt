package ru.sibwaf.inuyama

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import org.eclipse.jetty.http.HttpMethod
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.common.ApiResponse
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.STATUS_OK
import ru.sibwaf.inuyama.common.STATUS_SERVER_ERROR
import ru.sibwaf.inuyama.common.STATUS_SESSION_ERROR
import ru.sibwaf.inuyama.common.StatefulApiRequest
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.torrent.QBittorrentClient
import ru.sibwaf.inuyama.torrent.TorrentClient

private val kodein = Kodein.lazy {
    bind<Gson>() with singleton { Gson() }
    bind<JsonParser>() with singleton { JsonParser() }

    bind<KeyKeeper>() with singleton { KeyKeeper() }

    bind<SessionManager>() with singleton { SessionManager() }

    bind<TorrentClient>() with singleton { QBittorrentClient(kodein) }

    bind<Javalin>() with singleton { Javalin.create() }
    bind<Int>("api-port") with singleton { instance<Javalin>().port() }

    bind<PairingManager>() with singleton { PairingManager(kodein) }
}

private inline fun <reified T> Context.decryptedBody(): T {
    val request = body<StatefulApiRequest>()

    val gson by kodein.instance<Gson>()
    val sessionManager by kodein.instance<SessionManager>()

    sessionManager.findSession(request.session) ?: throw SessionException()

    return gson.fromJson(body())
}

private val insecurePaths = listOf("/ping", "/bind-session")

fun main() {
    val gson by kodein.instance<Gson>()

    JavalinJson.fromJsonMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>) = gson.fromJson(json, targetClass)
    }
    JavalinJson.toJsonMapper = object : ToJsonMapper {
        override fun map(obj: Any) = gson.toJson(obj)
    }

    val logger = LoggerFactory.getLogger("Main")

    val sessionManager by kodein.instance<SessionManager>()

    val torrentClient by kodein.instance<TorrentClient>()

    val javalin by kodein.instance<Javalin>()
    javalin.start().apply {
        get("/ping") {
            it.json(ApiResponse(STATUS_OK))
        }

        post("/bind-session") {
            val request = it.body<BindSessionApiRequest>()

            // TODO: encrypt session id with client's public key
            val session = sessionManager.createSession()
            it.json(BindSessionApiResponse(session))
        }

        post("/download-torrent") {
            val request = it.decryptedBody<TorrentDownloadApiRequest>()
            torrentClient.download(request.magnet, request.path)
            it.json(ApiResponse(STATUS_OK))
        }

        // TODO: make it more declarative
        after { ctx ->
            if (HttpMethod.POST.`is`(ctx.method())) {
                return@after
            }

            if (insecurePaths.none { ctx.path().startsWith(it) }) {
                // TODO: encrypt response
            }
        }

        exception<SessionException> { _, ctx ->
            ctx.json(ApiResponse(STATUS_SESSION_ERROR))
        }

        exception<Exception> { e, ctx ->
            logger.error("Caught an unhandled exception", e)
            ctx.json(ApiResponse(STATUS_SERVER_ERROR))
        }
    }

    kodein.direct.instance<PairingManager>().start()
}