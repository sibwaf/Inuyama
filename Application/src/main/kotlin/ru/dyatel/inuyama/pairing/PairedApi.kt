package ru.dyatel.inuyama.pairing

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.MediaTypes
import ru.sibwaf.inuyama.common.utilities.await
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.RemoteService
import java.net.ConnectException
import java.security.PublicKey
import javax.crypto.SecretKey

private data class Session(val token: String, val key: SecretKey)

private class PairedApiRequestManager(override val kodein: Kodein) : KodeinAware {

    private data class ServerConnection(val address: String, val key: PublicKey, var session: Session?)

    private val gson by instance<Gson>()

    private val networkManager by instance<NetworkManager>()
    private val pairingManager by instance<PairingManager>()

    private var serverConnection: ServerConnection? = null

    private val lock = Mutex()

    private suspend fun discoverServer(): ServerConnection {
        val server = pairingManager.findPairedServer() ?: throw PairedServerNotAvailableException()
        return ServerConnection(
                address = "http://${server.address.hostAddress}:${server.port}",
                key = server.key,
                session = null
        )
    }

    private suspend fun createSession(): Session {
        val serverConnection = serverConnection ?: throw PairedServerNotAvailableException("Can't create a session without a connection")
        val deviceKeyPair = pairingManager.deviceKeyPair

        val challenge = Encoding.stringToBytes(CommonUtilities.generateRandomString(64, CommonUtilities.ALPHABET_ALNUM))
        val request = BindSessionApiRequest(
                key = Encoding.encodeBase64(Encoding.encodeRSAPublicKey(deviceKeyPair.public)),
                challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, serverConnection.key))
        )

        val response = baseRequest("/bind-session", false, BindSessionApiResponse::class.java) {
            post(gson.toJson(request).toRequestBody(MediaTypes.APPLICATION_JSON))
        }

        val solvedChallenge = Cryptography.decryptRSA(Encoding.decodeBase64(response.challenge), deviceKeyPair.private)
        if (!challenge.contentEquals(solvedChallenge)) {
            throw PairedSessionException("Server failed to solve challenge") // TODO: critical
        }

        return Session(
                token = Encoding.bytesToString(Cryptography.decryptRSA(Encoding.decodeBase64(response.token), deviceKeyPair.private)),
                key = Encoding.decodeAESKey(Cryptography.decryptRSA(Encoding.decodeBase64(response.key), deviceKeyPair.private))
        )
    }

    private suspend fun <T> baseRequest(
            url: String,
            requiresSession: Boolean,
            responseType: Class<T>,
            init: Request.Builder.(Session?) -> Unit
    ): T {
        val serverConnection = serverConnection ?: throw PairedServerNotAvailableException("No cached server connections")
        val session = serverConnection.session
        if (requiresSession && session == null) {
            throw PairedSessionException("Session is required, but not bound")
        }

        val requestBuilder = Request.Builder()
                .url("${serverConnection.address}$url")

        if (requiresSession) {
            requestBuilder.header("Authorization", "Bearer ${session!!.token}")
        }

        requestBuilder.init(session)

        val request = networkManager.getHttpClient(true)
                .newCall(requestBuilder.build())

        val awaitedResponse = try {
            request.await()
        } catch (e: ConnectException) {
            throw PairedServerNotAvailableException("Failed to connect to server")
        }

        return awaitedResponse.use { response ->
            when (response.code) {
                200 -> {
                    var body = response.body!!.string()
                    if (requiresSession) {
                        body = body
                                .let { Encoding.decodeBase64(it) }
                                .let { Cryptography.decryptAES(it, session!!.key) }
                                .let { Encoding.bytesToString(it) }
                    }

                    gson.fromJson(body, responseType)
                }
                401 -> throw PairedSessionException("Token was rejected")
                else -> throw PairedApiException("Bad HTTP status ${response.code}")
            }
        }
    }

    suspend fun <T> request(url: String, responseType: Class<T>, init: Request.Builder.(Session?) -> Unit): T {
        lock.withLock {
            var allowServerDiscovery = true
            var allowSessionCreation = true

            var requester: suspend () -> T = { baseRequest(url, true, responseType, init) }

            while (true) {
                try {
                    return requester()
                } catch (e: Exception) {
                    if (e is PairedServerNotAvailableException && allowServerDiscovery) {
                        allowServerDiscovery = false

                        val oldSession = serverConnection?.session
                        serverConnection = discoverServer().apply { session = oldSession }

                        continue
                    }

                    if (e is PairedSessionException && allowSessionCreation) {
                        allowServerDiscovery = false
                        allowSessionCreation = false

                        val oldRequester = requester
                        requester = {
                            serverConnection!!.session = createSession()
                            oldRequester()
                        }

                        continue
                    }

                    throw e
                }
            }
        }
    }
}

class PairedApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private val gson by instance<Gson>()

    override val networkManager by instance<NetworkManager>()
    private val pairedApiRequestManager by lazy { PairedApiRequestManager(kodein) }

    private suspend inline fun <reified T> get(url: String, crossinline init: Request.Builder.() -> Unit = {}): T {
        return pairedApiRequestManager.request(url, T::class.java) {
            get()
            init()
        }
    }

    private suspend inline fun <reified T> post(url: String, data: Any? = null, crossinline init: Request.Builder.() -> Unit = {}): T {
        return pairedApiRequestManager.request(url, T::class.java) { session ->
            val body = data?.let { gson.toJson(data) }
                    ?.let { Encoding.stringToBytes(it) }
                    ?.let { Cryptography.encryptAES(it, session!!.key) }
                    ?.let { Encoding.encodeBase64(it) }
                    ?: ""

            post(body.toRequestBody(MediaTypes.TEXT_PLAIN))
            init()
        }
    }

    fun downloadTorrent(magnet: String, path: String) {
        runBlocking {
            post<Unit>("/download-torrent", TorrentDownloadApiRequest(magnet, path))
        }
    }

    override fun getName(context: Context): String = context.getString(R.string.module_pairing)

    private data class EchoRequest(val data: String)

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = EchoRequest(CommonUtilities.generateRandomString(16, CommonUtilities.ALPHABET_ALNUM))
            val response = post<EchoRequest>("/echo", request)
            return request == response
        } catch (e: Exception) {
            false
        }
    }
}