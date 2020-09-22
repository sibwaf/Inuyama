package ru.dyatel.inuyama.pairing

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.jsoup.Connection
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import java.net.ConnectException
import java.security.PublicKey
import java.util.Arrays
import javax.crypto.SecretKey

private data class Session(val token: String, val key: SecretKey)

private class PairedApiRequestManager(override val kodein: Kodein) : KodeinAware {

    private data class ServerConnection(val address: String, val key: PublicKey, var session: Session?)

    private val gson by instance<Gson>()

    private val networkManager by instance<NetworkManager>()
    private val pairingManager by instance<PairingManager>()

    private var serverConnection: ServerConnection? = null

    private val lock = Any()

    private fun discoverServer(): ServerConnection {
        val server = runBlocking { pairingManager.findPairedServer() } ?: throw PairedServerNotAvailableException()
        return ServerConnection(
                address = "http://${server.address.hostAddress}:${server.port}",
                key = server.key,
                session = null
        )
    }

    private fun createSession(): Session {
        val serverConnection = serverConnection ?: throw PairedServerNotAvailableException("Can't create a session without a connection")
        val deviceKeyPair = pairingManager.deviceKeyPair

        val challenge = Encoding.stringToBytes(CommonUtilities.generateRandomString(64, CommonUtilities.ALPHABET_ALNUM))
        val request = BindSessionApiRequest(
                key = Encoding.encodeBase64(Encoding.encodeRSAPublicKey(deviceKeyPair.public)),
                challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, serverConnection.key))
        )

        val response = baseRequest("/bind-session", Connection.Method.POST, false, BindSessionApiResponse::class.java) {
            requestBody(gson.toJson(request))
            header("Content-Type", "application/json")
        }

        val solvedChallenge = Cryptography.decryptRSA(Encoding.decodeBase64(response.challenge), deviceKeyPair.private)
        if (!Arrays.equals(challenge, solvedChallenge)) {
            throw PairedSessionException("Server failed to solve challenge") // TODO: critical
        }

        return Session(
                token = Encoding.bytesToString(Cryptography.decryptRSA(Encoding.decodeBase64(response.token), deviceKeyPair.private)),
                key = Encoding.decodeAESKey(Cryptography.decryptRSA(Encoding.decodeBase64(response.key), deviceKeyPair.private))
        )
    }

    private fun <T> baseRequest(
            url: String,
            method: Connection.Method,
            requiresSession: Boolean,
            responseType: Class<T>,
            init: Connection.(Session?) -> Unit
    ): T {
        val serverConnection = serverConnection ?: throw PairedServerNotAvailableException("No cached server connections")
        val session = serverConnection.session
        if (requiresSession && session == null) {
            throw PairedSessionException("Session is required, but not bound")
        }

        val request = networkManager.createJsoupConnection("${serverConnection.address}$url", true)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .method(method)

        if (requiresSession) {
            request.header("Authorization", "Bearer ${session!!.token}")
        }

        val response = try {
            request.apply { init(session) }.execute()
        } catch (e: ConnectException) {
            throw PairedServerNotAvailableException("Failed to connect to server")
        }

        when (response.statusCode()) {
            200 -> {
                var body = response.body()
                if (requiresSession) {
                    body = body
                            .let { Encoding.decodeBase64(it) }
                            .let { Cryptography.decryptAES(it, session!!.key) }
                            .let { Encoding.bytesToString(it) }
                }

                return gson.fromJson(body, responseType)
            }
            401 -> throw PairedSessionException("Token was rejected")
            else -> throw PairedApiException("Bad HTTP status ${response.statusCode()}")
        }
    }

    fun <T> request(url: String, method: Connection.Method, responseType: Class<T>, init: Connection.(Session?) -> Unit): T {
        synchronized(lock) {
            var allowServerDiscovery = true
            var allowSessionCreation = true

            var requester: () -> T = { baseRequest(url, method, true, responseType, init) }

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

    private inline fun <reified T> get(url: String, crossinline init: Connection.() -> Unit = {}): T {
        return pairedApiRequestManager.request(url, Connection.Method.GET, T::class.java) {
            init()
        }
    }

    private inline fun <reified T> post(url: String, data: Any? = null, crossinline init: Connection.() -> Unit = {}): T {
        return pairedApiRequestManager.request(url, Connection.Method.POST, T::class.java) { session ->
            data?.let { gson.toJson(data) }
                    ?.let { Encoding.stringToBytes(it) }
                    ?.let { Cryptography.encryptAES(it, session!!.key) }
                    ?.let { Encoding.encodeBase64(it) }
                    ?.let { requestBody(it) }

            header("Content-Type", "text/plain")

            init()
        }
    }

    fun downloadTorrent(magnet: String, path: String) {
        post<Unit>("/download-torrent", TorrentDownloadApiRequest(magnet, path))
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