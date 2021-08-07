package ru.sibwaf.inuyama.web

import io.javalin.Javalin
import ru.sibwaf.inuyama.KeyKeeper
import ru.sibwaf.inuyama.SessionManager
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.web.SecurityFilter.Companion.decryptBodyAs

class MainWebHandler(
    private val keyKeeper: KeyKeeper,
    private val sessionManager: SessionManager
) : WebHandler {

    override val insecurePaths = setOf("/ping", "/bind-session")

    override fun install(javalin: Javalin) {
        javalin.apply {
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
                val request = ctx.decryptBodyAs<Any>()
                ctx.json(request)
            }
        }
    }
}
