package ru.dyatel.inuyama.ruranobe

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class RuranobeApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private companion object {
        const val host = "http://ruranobe.ru"
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

    fun fetchProjects(): List<RuranobeProject> {
        try {
            val json = Jsoup.connect("$host/api/projects")
                    .ignoreContentType(true)
                    .data("fields", "projectId,title,author,works,status,translationStatus,issueStatus")
                    .get()
                    .text()

            val typeToken = object : TypeToken<List<RuranobeProject>>() {}.type
            return gson.fromJson<List<RuranobeProject>>(json, typeToken)
        } catch (e: Exception) {
            throw e
        }
    }

    fun fetchVolumes(project: RuranobeProject): List<RuranobeVolume> {
        try {
            val json = Jsoup.connect("$host/api/projects/${project.id}/volumes")
                    .ignoreContentType(true)
                    .data("fields", "volumeId,url,imageUrl,nameTitle,volumeStatus,lastUpdateDate,lastEditDate")
                    .get()
                    .text()

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
                                .map { it.asJsonObject["url"].asString }
                                .lastOrNull()
                                ?.let { if (it.startsWith("//")) "http:$it" else it }

                        parsed.updateDatetime = listOfNotNull(volume["lastUpdateDate"], volume["lastEditDate"])
                                .map { datetimeFormat.parse(it.asString).asDateTime }
                                .sorted()
                                .lastOrNull()

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
