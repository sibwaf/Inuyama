package ru.sibwaf.inuyama

import com.google.gson.Gson
import io.javalin.Javalin
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.sibwaf.inuyama.common.ApiResponse
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.STATUS_OK
import ru.sibwaf.inuyama.common.STATUS_SERVER_ERROR
import ru.sibwaf.inuyama.common.STATUS_SESSION_ERROR
import ru.sibwaf.inuyama.common.StatefulApiRequest
import ru.sibwaf.inuyama.torrent.TorrentClient
import ru.sibwaf.inuyama.torrent.TransmissionClient
import ru.sibwaf.inuyama.torrent.TransmissionConfiguration

private val kodein = Kodein.lazy {
    bind<Gson>() with singleton { Gson() }

    bind<KeyKeeper>() with singleton { KeyKeeper() }

    bind<SessionManager>() with singleton { SessionManager() }

    bind<TransmissionConfiguration>() with singleton { TransmissionConfiguration() }
    bind<TorrentClient>() with singleton { TransmissionClient(kodein) }

    bind<Javalin>() with singleton { Javalin.create() }
    bind<Int>("api-port") with singleton { instance<Javalin>().port() }

    bind<PairingManager>() with singleton { PairingManager(kodein) }
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

    val sessionManager by kodein.instance<SessionManager>()

    val javalin by kodein.instance<Javalin>()
    javalin.start().apply {
        before { ctx ->
            if (insecurePaths.none { ctx.path().startsWith(it) }) {
                val request = ctx.body<StatefulApiRequest>()

                // TODO: add session attribute to the request
                val session = sessionManager.findSession(request.session) ?: throw SessionException()
            }
        }

        get("/ping") {
            it.json(ApiResponse(STATUS_OK))
        }

        post("/bind-session") {
            val request = it.body<BindSessionApiRequest>()

            // TODO: encode session id with client's public key
            val session = sessionManager.createSession()
            it.json(BindSessionApiResponse(session))
        }

        post("/download-torrent") {
            // TODO
            it.json(ApiResponse(STATUS_OK))
        }

        exception<SessionException> { _, ctx ->
            ctx.json(ApiResponse(STATUS_SESSION_ERROR))
        }

        exception<Exception> { _, ctx ->
            ctx.json(ApiResponse(STATUS_SERVER_ERROR))
        }
    }

    kodein.direct.instance<PairingManager>().start()
}