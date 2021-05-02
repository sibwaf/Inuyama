package ru.dyatel.inuyama.ruranobe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.SERVICE_RURANOBE
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.utilities.fromJson
import ru.sibwaf.inuyama.common.utilities.asDateTime
import ru.sibwaf.inuyama.common.utilities.await
import sibwaf.inuyama.app.common.NetworkManager
import sibwaf.inuyama.app.common.ProxyableRemoteService
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class RuranobeApi(override val kodein: Kodein) : KodeinAware, ProxyableRemoteService {

    private companion object {
        const val host = "https://ruranobe.ru"
        const val tooManyRequestsDelay = 2500L

        val datetimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss Z", Locale.US)
    }

    override val serviceId = SERVICE_RURANOBE

    override val networkManager by instance<NetworkManager>()

    private val gson by instance<Gson>()
    private val jsonParser by instance<JsonParser>()

    override fun getName(context: Context) = context.getString(R.string.module_ruranobe)!!

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = Request.Builder().url(host).build()
            val response = getHttpClient(false).newCall(request).await()
            return response.use { it.isSuccessful }
        } catch (e: IOException) {
            false
        }
    }

    private fun handleResponseCode(response: Connection.Response): Boolean {
        val code = response.statusCode()

        // TODO: replace with HttpURLConnection constant when it is present
        if (code == 429) {
            Thread.sleep(tooManyRequestsDelay)
            return true
        }

        if (code < HttpURLConnection.HTTP_OK || response.statusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw HttpStatusException("HTTP error fetching URL", code, response.url().toString())
        }

        return false
    }

    fun fetchProjects(): List<RuranobeProject> {
        try {
            val response = createConnection("$host/api/projects", false)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .data("fields", "projectId,url,title,nameRomaji,author,works,status,translationStatus,issueStatus")
                    .execute()

            if (handleResponseCode(response)) {
                return fetchProjects()
            }

            val json = response.body()

            val typeToken = object : TypeToken<List<RuranobeProject>>() {}.type
            return gson.fromJson<List<RuranobeProject>>(json, typeToken)
                    .onEach { it.status = it.status.toLowerCase() }
        } catch (e: Exception) {
            throw e
        }
    }

    fun fetchVolumes(project: RuranobeProject): List<RuranobeVolume> {
        try {
            val response = createConnection("$host/api/projects/${project.id}/volumes", false)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .data("fields", "volumeId,url,imageThumbnail,nameTitle,volumeStatus,lastUpdateDate,lastEditDate")
                    .execute()

            if (handleResponseCode(response)) {
                return fetchVolumes(project)
            }

            val json = response.body()

            return jsonParser.parse(json).asJsonArray
                    .map { it.asJsonObject!! }
                    .mapIndexed { index, volume ->
                        val parsed = gson.fromJson<RuranobeVolume>(volume)

                        parsed.coverUrl = volume.getAsJsonArray("covers")
                                .map { it.asJsonObject["thumbnail"].asString }
                                .firstOrNull()
                                ?.let { if (it.startsWith("//")) "http:$it" else it }
                                ?.replace("%dpx", "240px")

                        parsed.updateDatetime = listOfNotNull(volume["lastUpdateDate"], volume["lastEditDate"])
                                .map { datetimeFormat.parse(it.asString).asDateTime }
                                .sorted()
                                .lastOrNull()

                        parsed.order = index
                        parsed.project.target = project

                        return@mapIndexed parsed
                    }
        } catch (e: Exception) {
            throw e
        }
    }

    fun getDownloadUrl(volume: RuranobeVolume, format: String): URL {
        try {
            return createConnection("$host/d/$format/${volume.url}", false)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .header("Referer", "$host/r/${volume.url}")
                    .execute()
                    .url()
        } catch (e: Exception) {
            throw e
        }
    }

    fun getProjectPageUrl(project: RuranobeProject) = "$host/r/${project.url}"
    fun getVolumePageUrl(volume: RuranobeVolume) = "$host/r/${volume.url}"

}
