package ru.dyatel.inuyama.ruranobe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.model.RuranobeProject
import ru.dyatel.inuyama.model.RuranobeVolume
import ru.dyatel.inuyama.utilities.asDateTime
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class RuranobeApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private companion object {
        const val host = "http://ruranobe.ru"
        const val tooManyRequestsDelay = 2500L

        val datetimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss Z", Locale.US)
    }

    private val gson by instance<Gson>()
    private val jsonParser by instance<JsonParser>()

    override fun getName(context: Context) = context.getString(R.string.module_ruranobe)!!

    override fun checkConnection(): Boolean {
        return try {
            Jsoup.connect(host).get()
            true
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
            val response = Jsoup.connect("$host/api/projects")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .data("fields", "projectId,title,author,works,status,translationStatus,issueStatus")
                    .execute()

            if (handleResponseCode(response)) {
                return fetchProjects()
            }

            val json = response.body()

            val typeToken = object : TypeToken<List<RuranobeProject>>() {}.type
            return gson.fromJson<List<RuranobeProject>>(json, typeToken)
        } catch (e: Exception) {
            throw e
        }
    }

    fun fetchVolumes(project: RuranobeProject): List<RuranobeVolume> {
        try {
            val response = Jsoup.connect("$host/api/projects/${project.id}/volumes")
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
                        val parsed = RuranobeVolume(
                                id = volume["volumeId"].asLong,
                                order = index,
                                url = volume["url"].asString,
                                title = volume["nameTitle"].asString,
                                status = volume["volumeStatus"].asString
                        )

                        parsed.coverUrl = volume.getAsJsonArray("covers")
                                .map { it.asJsonObject["thumbnail"].asString }
                                .firstOrNull()
                                ?.let { if (it.startsWith("//")) "http:$it" else it }
                                ?.replace("%dpx", "240px")

                        parsed.updateDatetime = listOfNotNull(volume["lastUpdateDate"], volume["lastEditDate"])
                                .map { datetimeFormat.parse(it.asString).asDateTime }
                                .sorted()
                                .lastOrNull()

                        parsed.project.target = project

                        return@mapIndexed parsed
                    }
        } catch (e: Exception) {
            throw e
        }
    }

    fun getDownloadUrl(volume: RuranobeVolume, format: String): URL {
        try {
            return Jsoup.connect("$host/d/$format/${volume.url}")
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .header("Referer", "$host/r/${volume.url}")
                    .execute()
                    .url()
        } catch (e: Exception) {
            throw e
        }
    }

}
