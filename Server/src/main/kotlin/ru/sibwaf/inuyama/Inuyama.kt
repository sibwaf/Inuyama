package ru.sibwaf.inuyama

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.json.FromJsonMapper
import io.javalin.json.JavalinJson
import io.javalin.json.ToJsonMapper
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.LoggerFactory
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.torrent.QBittorrentClient
import ru.sibwaf.inuyama.torrent.TorrentClient
import java.nio.file.Files
import java.nio.file.Paths

private val kodein = Kodein.lazy {
    bind<Gson>() with singleton { Gson() }
    bind<JsonParser>() with singleton { JsonParser() }

    bind<InuyamaConfiguration>() with singleton {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .create()

        val configurationPath = Paths.get("configuration.json")
        return@singleton if (Files.exists(configurationPath)) {
            val configurationText = Files.readAllLines(configurationPath).joinToString("\n")
            gson.fromJson(configurationText)
        } else {
            InuyamaConfiguration()
        }
    }

    bind<KeyKeeper>() with singleton { KeyKeeper() }
    bind<SessionManager>() with singleton { SessionManager() }
    bind<PairingManager>() with singleton { PairingManager(kodein) }

    bind<TorrentClient>() with singleton { QBittorrentClient(kodein) }
}

private const val ATTRIBUTE_SESSION = "sibwaf.inuyama.session"

private inline fun <reified T> Context.decryptedBody(): T {
    val session = attribute<Session>(ATTRIBUTE_SESSION)!!
    val gson by kodein.instance<Gson>()
    return body()
        .let { Encoding.decodeBase64(it) }
        .let { Cryptography.decryptAES(it, session.key) }
        .let { Encoding.bytesToString(it) }
        .let { gson.fromJson(it) }
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

    val configuration by kodein.instance<InuyamaConfiguration>()
    val keyKeeper by kodein.instance<KeyKeeper>()
    val sessionManager by kodein.instance<SessionManager>()
    val torrentClient by kodein.instance<TorrentClient>()

    Javalin.create()
        .port(configuration.serverPort)
        .start()
        .apply {
            get("/ping") {}

            post("/bind-session") { ctx ->
                val request = ctx.body<BindSessionApiRequest>()
                val clientKey = Encoding.decodeRSAPublicKey(Encoding.decodeBase64(request.key))
                val challenge = Cryptography.decryptRSA(Encoding.decodeBase64(request.challenge), keyKeeper.keyPair.private)

                val session = sessionManager.createSession()

                val response = BindSessionApiResponse(
                    challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, clientKey)),
                    token = Encoding.encodeBase64(Cryptography.encryptRSA(Encoding.stringToBytes(session.token), clientKey)),
                    key = Encoding.encodeBase64(Cryptography.encryptRSA(Encoding.encodeAESKey(session.key), clientKey))
                )

                ctx.json(response)
            }

            post("/echo") { ctx ->
                val request = ctx.decryptedBody<Any>()
                ctx.json(request)
            }

            post("/download-torrent") { ctx ->
                val request = ctx.decryptedBody<TorrentDownloadApiRequest>()
                torrentClient.download(request.magnet, request.path)
            }

            before { ctx ->
                if (insecurePaths.any { ctx.path().startsWith(it) }) {
                    return@before
                }

                val session = ctx.header("Authorization")
                    ?.takeIf { it.startsWith("Bearer ") }
                    ?.removePrefix("Bearer ")
                    ?.let { sessionManager.findSession(it) }
                    ?: throw SessionException()

                ctx.attribute(ATTRIBUTE_SESSION, session)
            }

            after { ctx ->
                val session = ctx.attribute<Session>(ATTRIBUTE_SESSION) ?: return@after
                val response = ctx.resultString() ?: return@after
                response.let { Encoding.stringToBytes(it) }
                    .let { Cryptography.encryptAES(it, session.key) }
                    .let { Encoding.encodeBase64(it) }
                    .let { ctx.result(it) }
                    .let { ctx.contentType("text/plain") }
            }

            exception<SessionException> { _, ctx ->
                ctx.status(401)
            }

            exception<Exception> { e, ctx ->
                logger.error("Caught an unhandled exception", e)
                ctx.status(500)
            }
        }

    kodein.direct.instance<PairingManager>().start()
}