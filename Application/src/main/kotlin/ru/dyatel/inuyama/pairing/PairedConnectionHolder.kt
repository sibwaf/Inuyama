package ru.dyatel.inuyama.pairing

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.MediaTypes
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import ru.sibwaf.inuyama.common.utilities.humanReadable
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import sibwaf.inuyama.app.common.NetworkManager
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicReference

class PairedConnectionHolder(
    private val networkManager: NetworkManager,
    private val pairingManager: PairingManager,
    private val gson: Gson,
) {

    private val logTag = javaClass.simpleName

    private val mutex = Mutex()

    private val server = AtomicReference<PairedServer>()
    private val session = AtomicReference<PairedSession>()

    suspend fun <T> withServer(block: suspend (PairedServer) -> T): T {
        var server: PairedServer?
        for (attempt in 0 until 2) {
            mutex.withLock {
                server = this.server.get()
                if (server == null) {
                    Log.d(logTag, "Discovering server")
                    server = pairingManager.findPairedServer() ?: throw PairedServerNotAvailableException("Failed to discover the server")

                    Log.d(logTag, "Discovered server ${server!!.host}:${server!!.port} (${server!!.key.humanReadable})")
                    this.server.set(server)
                }
            }

            try {
                return block(server!!)
            } catch (e: PairedServerNotAvailableException) {
                Log.d(logTag, "Server ${server!!.host}:${server!!.port} seems dead, resetting")
                this.server.compareAndSet(server, null)
            }
        }

        throw PairedServerNotAvailableException("Too many server discovery retries")
    }

    suspend fun <T> withSession(block: suspend (PairedServer, PairedSession) -> T): T {
        return withServer { server ->
            var session: PairedSession?
            for (attempt in 0 until 2) {
                mutex.withLock {
                    session = this.session.get()
                    if (session == null) {
                        Log.d(logTag, "Creating new session")
                        session = createSession(server)

                        this.session.set(session)
                    }
                }

                try {
                    return@withServer block(server, session!!)
                } catch (e: PairedSessionException) {
                    Log.d(logTag, "Session ${session!!.token} seems dead, resetting")
                    this.session.compareAndSet(session, null)
                }
            }

            throw PairedSessionException("Too many session setup retries")
        }
    }

    private suspend fun createSession(server: PairedServer): PairedSession {
        val httpClient = networkManager.getHttpClient(trustedOnly = true)

        val deviceKeyPair = pairingManager.deviceKeyPair

        val challenge = Encoding.stringToBytes(CommonUtilities.generateRandomString(64, CommonUtilities.ALPHABET_ALNUM))
        val request = BindSessionApiRequest(
            key = Encoding.encodeBase64(Encoding.encodeRSAPublicKey(deviceKeyPair.public)),
            challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, server.key)),
            deviceId = pairingManager.deviceIdentifier
        )

        val response = try {
            httpClient
                .newCall(
                    Request.Builder()
                        .url("${server.url}/bind-session")
                        .post(gson.toJson(request).toRequestBody(MediaTypes.APPLICATION_JSON))
                        .build()
                )
                .await()
                .use {
                    it.successOrThrow()
                    gson.fromJson<BindSessionApiResponse>(it.body!!.charStream())
                }
        } catch (e: ConnectException) {
            throw PairedServerNotAvailableException("Failed to connect to server")
        }

        val solvedChallenge = Cryptography.decryptRSA(Encoding.decodeBase64(response.challenge), deviceKeyPair.private)
        if (!challenge.contentEquals(solvedChallenge)) {
            throw PairedApiException("Server failed to solve challenge") // TODO: critical
        }

        return PairedSession(
            token = Encoding.bytesToString(Cryptography.decryptRSA(Encoding.decodeBase64(response.token), deviceKeyPair.private)),
            key = Encoding.decodeAESKey(Cryptography.decryptRSA(Encoding.decodeBase64(response.key), deviceKeyPair.private))
        )
    }
}
