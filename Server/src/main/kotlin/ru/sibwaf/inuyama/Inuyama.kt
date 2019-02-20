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
import ru.sibwaf.inuyama.common.STATUS_OK
import ru.sibwaf.inuyama.common.STATUS_SERVER_ERROR
import ru.sibwaf.inuyama.torrent.TorrentClient
import ru.sibwaf.inuyama.torrent.TransmissionClient
import ru.sibwaf.inuyama.torrent.TransmissionConfiguration

val kodein = Kodein.lazy {
    bind<Gson>() with singleton { Gson() }

    bind<TransmissionConfiguration>() with singleton { TransmissionConfiguration() }
    bind<TorrentClient>() with singleton { TransmissionClient(kodein) }

    bind<Javalin>() with singleton { Javalin.create() }
    bind<Int>("api-port") with singleton { instance<Javalin>().port() }

    bind<PairingManager>() with singleton { PairingManager(kodein) }
}

fun main() {
    val gson by kodein.instance<Gson>()

    JavalinJson.fromJsonMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>) = gson.fromJson(json, targetClass)
    }
    JavalinJson.toJsonMapper = object : ToJsonMapper {
        override fun map(obj: Any) = gson.toJson(obj)
    }

    val javalin by kodein.instance<Javalin>()
    javalin.start().apply {
        get("/ping") {
            it.json(ApiResponse(STATUS_OK))
        }

        post("/download-torrent") {
            // TODO
            it.json(ApiResponse(STATUS_OK))
        }

        exception<Exception> { _, ctx ->
            ctx.json(ApiResponse(STATUS_SERVER_ERROR))
        }
    }

    kodein.direct.instance<PairingManager>().start()
}