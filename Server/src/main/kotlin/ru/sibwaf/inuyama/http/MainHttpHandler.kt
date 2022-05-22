package ru.sibwaf.inuyama.http

import io.javalin.Javalin
import ru.sibwaf.inuyama.KeyKeeper
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBodyAs
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptedBody
import ru.sibwaf.inuyama.pairing.PairedSessionManager

class MainHttpHandler(
    private val keyKeeper: KeyKeeper,
    private val pairedSessionManager: PairedSessionManager
) : HttpHandler {

    override fun Javalin.install() {
        pairedSubroute {
            get("/ping") {}

            post("/bind-session") { ctx ->
                val request = ctx.decryptBodyAs<BindSessionApiRequest>()
                val clientKey = Encoding.decodeRSAPublicKey(Encoding.decodeBase64(request.key))
                val challenge = Cryptography.decryptRSA(Encoding.decodeBase64(request.challenge), keyKeeper.keyPair.private)

                val session = pairedSessionManager.createSession(request.deviceId)

                val response = BindSessionApiResponse(
                    challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, clientKey)),
                    token = Encoding.encodeBase64(Cryptography.encryptRSA(Encoding.stringToBytes(session.token), clientKey)),
                    key = Encoding.encodeBase64(Cryptography.encryptRSA(Encoding.encodeAESKey(session.key), clientKey))
                )

                ctx.json(response)
            }

            post("/echo") { ctx ->
                ctx.result(ctx.decryptedBody())
            }
        }
    }
}
