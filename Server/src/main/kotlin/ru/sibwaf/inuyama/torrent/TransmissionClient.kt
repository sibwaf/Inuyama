package ru.sibwaf.inuyama.torrent

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

private const val SESSION_HEADER = "X-Transmission-Session-Id"

class TransmissionClient(override val kodein: Kodein) : KodeinAware, TorrentClient {

    private val gson by instance<Gson>()
    private val parser by instance<JsonParser>()

    private val configuration by instance<TransmissionConfiguration>()

    private var session: String? = null

    private fun connect(): Connection {
        val url = with(configuration) { URI.create("http://$host:$port/$path").normalize().toString() }

        val connection = Jsoup.connect(url)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .method(Connection.Method.POST)

        val username = configuration.username
        val password = configuration.password

        val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        connection.header("Authorization", "Basic $credentials")

        session?.let { connection.header(SESSION_HEADER, it) }

        return connection
    }

    fun executeRaw(request: TransmissionRequest): JsonObject? {
        try {
            val connection = connect()
            connection.requestBody(gson.toJson(request))

            var response = try {
                connection.execute()
            } catch (e: IOException) {
                throw TransmissionException("Failed to connect")
            }

            if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw TransmissionAuthorizationException()
            }

            if (response.statusCode() == HttpURLConnection.HTTP_CONFLICT) {
                session = response.header(SESSION_HEADER)
                    ?: throw TransmissionException("Failed to get session ID after authorization!")

                response = try {
                    connection.header(SESSION_HEADER, session).execute()
                } catch (e: IOException) {
                    throw TransmissionException("Failed to connect")
                }
            }

            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw TransmissionException("Request failed with HTTP code ${response.statusCode()}")
            }

            val data = parser.parse(response.body()).asJsonObject

            val result = data["result"]?.asString ?: throw TransmissionException("Unexpected response")
            if (result != "success") {
                throw TransmissionMethodException(result)
            }

            return data["arguments"]?.asJsonObject
        } catch (e: Exception) {
            if (e is TransmissionException) {
                throw e
            }
            throw TransmissionException(e)
        }
    }

    override fun download(magnet: String, directory: String?) {
        val arguments = mapOf("filename" to magnet, "download-dir" to directory)
        executeRaw(TransmissionRequest("torrent-add", arguments))
    }

}

data class TransmissionRequest(val method: String, val arguments: Map<String, String?>? = null)
