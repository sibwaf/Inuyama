package ru.dyatel.inuyama.pairing

import android.content.Context
import com.google.gson.Gson
import org.jsoup.Connection
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.utilities.fromJson
import ru.sibwaf.inuyama.common.BindSessionApiRequest
import ru.sibwaf.inuyama.common.BindSessionApiResponse
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import java.net.ConnectException
import java.util.Arrays
import javax.crypto.SecretKey

class PairedApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private data class Session(val address: String, val token: String, val key: SecretKey)

    private val gson by instance<Gson>()

    private val pairingManager by instance<PairingManager>()
    override val networkManager by instance<NetworkManager>()

    private var session: Session? = null

    private inline fun <reified T> request(
            url: String,
            method: Connection.Method,
            allowNewSession: Boolean,
            crossinline init: Connection.() -> Unit
    ): T {
        if (pairingManager.pairedServer == null) {
            session = null
            throw PairedSessionException("No device is paired")
        }

        var allowNewSession0 = allowNewSession
        if (session == null) {
            bindSession()
            allowNewSession0 = false
        }

        val requester: () -> T = requester@{
            val session = session ?: throw PairedSessionException("No session is bound")

            val response = createConnection("http://${session.address}$url", true)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .header("Authorization", "Bearer ${session.token}")
                    .apply(init)
                    .method(method)
                    .execute()

            when (response.statusCode()) {
                200 -> return@requester gson.fromJson<T>(response.body()) // TODO: decrypt
                401 -> throw PairedSessionException("Token was rejected")
                else -> throw PairedApiException("Bad HTTP status ${response.statusCode()}")
            }
        }

        return try {
            requester()
        } catch (e: Exception) {
            if (!allowNewSession0) {
                throw e
            }

            if (e !is ConnectException && e !is PairedSessionException) {
                throw e
            }

            bindSession()
            requester()
        }
    }

    private inline fun <reified T> get(
            url: String,
            allowNewSession: Boolean = true,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.GET, allowNewSession, init)
    }

    private inline fun <reified T> post(
            url: String,
            data: Any? = null,
            allowNewSession: Boolean = true,
            crossinline init: Connection.() -> Unit = {}
    ): T {
        return request(url, Connection.Method.POST, allowNewSession) {
            if (data != null) {
                val body = gson.toJson(data)
                // TODO: encrypt body
                requestBody(body)
            }

            init()
        }
    }

    private fun bindSession() {
        try {
            val server = pairingManager.findPairedServer() ?: throw PairedSessionException("Paired device is not available")
            val address = "${server.address.hostAddress}:${server.port}"

            val deviceKeyPair = pairingManager.deviceKeyPair

            val challenge = Encoding.stringToBytes(CommonUtilities.generateRandomString(64, CommonUtilities.ALPHABET_ALNUM))
            val request = BindSessionApiRequest(
                    key = Encoding.encodeBase64(Encoding.encodeRSAPublicKey(deviceKeyPair.public)),
                    challenge = Encoding.encodeBase64(Cryptography.encryptRSA(challenge, server.key))
            )

            val response = createConnection("http://$address/bind-session", true)
                    .ignoreContentType(true)
                    .requestBody(gson.toJson(request))
                    .method(Connection.Method.POST)
                    .execute()
                    .body()
                    .let { gson.fromJson<BindSessionApiResponse>(it) }

            val solvedChallenge = Cryptography.decryptRSA(Encoding.decodeBase64(response.challenge), deviceKeyPair.private)
            if (!Arrays.equals(challenge, solvedChallenge)) {
                throw PairedSessionException("Server failed to solve challenge") // TODO: critical
            }

            session = Session(
                    address = address,
                    token = Encoding.bytesToString(Cryptography.decryptRSA(Encoding.decodeBase64(response.token), deviceKeyPair.private)),
                    key = Encoding.decodeAESKey(Cryptography.decryptRSA(Encoding.decodeBase64(response.key), deviceKeyPair.private))
            )
        } catch (e: Exception) {
            session = null

            if (e is PairedSessionException) {
                throw e
            }

            throw PairedSessionException("Failed to create a session", e)
        }
    }

    fun downloadTorrent(magnet: String, path: String) {
        post<Unit>("/download-torrent", TorrentDownloadApiRequest(magnet, path))
    }

    override fun getName(context: Context): String = context.getString(R.string.module_pairing)

    override fun checkConnection(): Boolean {
        return try {
            get<Unit>("/ping")
            true
        } catch (e: Exception) {
            false
        }
    }
}