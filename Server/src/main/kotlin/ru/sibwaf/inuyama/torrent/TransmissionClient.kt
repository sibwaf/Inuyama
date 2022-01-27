package ru.sibwaf.inuyama.torrent

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.sibwaf.inuyama.TorrentClientConfiguration
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.MediaTypes
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import ru.sibwaf.inuyama.fromJson
import java.util.concurrent.atomic.AtomicReference

private const val SESSION_HEADER = "X-Transmission-Session-Id"

class TransmissionClient(
    private val configuration: TorrentClientConfiguration,
    private val httpClient: OkHttpClient,
    private val gson: Gson
) : TorrentClient {

    private val credentials = with(configuration) { Encoding.encodeBase64(Encoding.stringToBytes("$username:$password")) }

    private val session = AtomicReference<String>()

    private suspend fun executeRaw(request: TransmissionRequest): JsonObject? {
        val requestBuilder = Request.Builder()
            .url(configuration.url)
            .post(gson.toJson(request).toRequestBody(MediaTypes.APPLICATION_JSON))
            .header("Authorization", "Basic $credentials")

        var canRefreshSession = true
        while (true) {
            val session = session.get()
            if (session != null) {
                requestBuilder.header(SESSION_HEADER, session)
            } else {
                requestBuilder.removeHeader(SESSION_HEADER)
            }

            val response = httpClient
                .newCall(requestBuilder.build())
                .await()

            if (response.code == 409 && canRefreshSession) {
                canRefreshSession = false
                this.session.compareAndSet(session, response.header(SESSION_HEADER))
                continue
            }

            return response.successOrThrow().use {
                val body = it.body?.let { body -> gson.fromJson<JsonObject>(body.charStream()) }

                val result = body?.get("result")?.asString
                if (result != "success") {
                    throw TorrentClientException("Non-successful request result: $result")
                }

                body["arguments"]?.asJsonObject
            }
        }
    }

    override suspend fun download(magnet: String, directory: String) {
        val arguments = mapOf("filename" to magnet, "download-dir" to directory)
        executeRaw(TransmissionRequest("torrent-add", arguments))
    }

}

private data class TransmissionRequest(val method: String, val arguments: Map<String, String?>? = null)
