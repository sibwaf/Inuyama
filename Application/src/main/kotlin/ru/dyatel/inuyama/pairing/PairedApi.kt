package ru.dyatel.inuyama.pairing

import android.content.Context
import com.google.gson.Gson
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import ru.dyatel.inuyama.R
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.common.utilities.CommonUtilities
import ru.sibwaf.inuyama.common.utilities.Cryptography
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.MediaTypes
import ru.sibwaf.inuyama.common.utilities.asRequestBody
import ru.sibwaf.inuyama.common.utilities.await
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.RemoteService
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.ConnectException

class PairedApi(
    private val pairedConnectionHolder: PairedConnectionHolder,
    override val networkManager: NetworkManager,
    private val gson: Gson,
) : RemoteService {

    suspend fun downloadTorrent(magnet: String, path: String) {
        val request = TorrentDownloadApiRequest(magnet, path)

        pairedConnectionHolder.withSession { server, session ->
            makeRequest {
                post(encryptBody(gson.toJson(request), session))
                withUrl(server, "/download-torrent")
                withAuth(session)
            }.close()
        }
    }

    suspend fun getBackupContent(module: String): InputStream {
        return pairedConnectionHolder.withSession { server, session ->
            val response = makeRequest {
                get()
                withUrl(server, "/backup/$module/content")
                withAuth(session)
            }

            decryptBody(response.body!!.byteStream(), session)
        }
    }

    suspend fun makeBackup(module: String, data: InputStream) {
        pairedConnectionHolder.withSession { server, session ->
            makeRequest {
                post(encryptBody(data, session))
                withUrl(server, "/backup/$module")
                withAuth(session)
            }.close()
        }
    }

    override fun getName(context: Context): String = context.getString(R.string.module_pairing)

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = CommonUtilities.generateRandomString(16, CommonUtilities.ALPHABET_ALNUM)

            val response = pairedConnectionHolder.withSession { server, session ->
                makeRequest {
                    post(encryptBody(request, session))
                    withUrl(server, "/echo")
                    withAuth(session)
                }.use {
                    Encoding.bytesToString(decryptBody(it.body!!.byteStream(), session).readBytes())
                }
            }

            return request == response
        } catch (e: Exception) {
            false
        }
    }

    private fun encryptBody(body: InputStream, session: PairedSession): RequestBody {
        return Cryptography.encryptAES(body, session.key).asRequestBody(MediaTypes.APPLICATION_OCTET_STREAM)
    }

    private fun encryptBody(body: String, session: PairedSession): RequestBody {
        return body
            .let { Encoding.stringToBytes(it) }
            .let { ByteArrayInputStream(it) }
            .let { encryptBody(it, session) }
    }

    private fun decryptBody(body: InputStream, session: PairedSession): InputStream {
        return Cryptography.decryptAES(body, session.key)
    }

    private suspend fun makeRequest(customize: Request.Builder.() -> Unit): Response {
        val request = Request.Builder().apply(customize).build()

        val response = try {
            getHttpClient(trustedOnly = true)
                .newCall(request)
                .await()
        } catch (e: ConnectException) {
            throw PairedServerNotAvailableException("Failed to connect to ${request.url.host}:${request.url.port}")
        }

        if (response.code == 200) {
            return response
        }

        response.closeQuietly()
        when (response.code) {
            401 -> throw PairedSessionException("Token was rejected")
            else -> throw PairedApiException("Bad HTTP status ${response.code}")
        }
    }

    private fun Request.Builder.withUrl(server: DiscoveredServer, path: String) = url("${server.url}$path")
    private fun Request.Builder.withAuth(session: PairedSession) = header("Authorization", "Bearer ${session.token}")
}
