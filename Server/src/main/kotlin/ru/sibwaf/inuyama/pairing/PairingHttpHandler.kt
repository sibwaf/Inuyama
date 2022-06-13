package ru.sibwaf.inuyama.pairing

import io.javalin.Javalin
import ru.sibwaf.inuyama.KeyKeeper
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.PingApiResponse
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBodyAs
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptedBody
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.encryptableResponseBody
import ru.sibwaf.inuyama.http.pairedSubroute
import ru.sibwaf.inuyama.http.subroute
import ru.sibwaf.inuyama.http.webSubroute

class PairingHttpHandler(
    private val keyKeeper: KeyKeeper,
    private val pairedSessionManager: PairedSessionManager,
    private val deviceManager: DeviceManager,
) : HttpHandler {

    override fun Javalin.install() {
        webSubroute {
            subroute("/devices") {
                get("/") {
                    it.json(deviceManager.listDevices())
                }
            }
        }

        pairedSubroute {
            get("/ping") { ctx ->
                val response = PingApiResponse(
                    key = Encoding.encodeBase64(Encoding.encodeRSAPublicKey(keyKeeper.keyPair.public))
                )

                ctx.json(response)
            }

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
                ctx.encryptableResponseBody = ctx.decryptedBody()
            }
        }
    }
}
