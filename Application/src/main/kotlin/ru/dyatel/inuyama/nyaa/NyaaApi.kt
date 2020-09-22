package ru.dyatel.inuyama.nyaa

import android.content.Context
import okhttp3.Request
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.ProxyableRemoteService
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.SERVICE_NYAA
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.sibwaf.inuyama.common.utilities.asDateTime
import ru.sibwaf.inuyama.common.utilities.await
import java.text.SimpleDateFormat
import java.util.Locale

class NyaaApi(override val kodein: Kodein) : KodeinAware, ProxyableRemoteService {

    private companion object {
        const val host = "https://nyaa.si"
        val idPattern = Regex("/(\\d+)$")
        val datetimeFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }

    override val serviceId = SERVICE_NYAA

    override val networkManager by instance<NetworkManager>()

    override fun getName(context: Context) = context.getString(R.string.module_nyaa)!!

    override suspend fun checkConnection(): Boolean {
        return try {
            val request = Request.Builder().url(host).build()
            val response = getHttpClient(false).newCall(request).await()
            return response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    fun query(query: String): List<NyaaTorrent> {
        try {
            return createConnection(host, false)
                    .data("page", "rss").data("f", "2").data("c", "0_0").data("q", query)
                    .parser(Parser.xmlParser())
                    .get()
                    .select("channel > item")
                    .map { parse(it) }
        } catch (e: Exception) {
            throw e // TODO: wrap into another exception
        }
    }

    private fun parse(element: Element): NyaaTorrent {
        try {
            val guid = element.getElementsByTag("guid").single().text()
            val id = idPattern.find(guid)
                    ?.let { it.groupValues[1].toLong() }
                    ?: throw Exception("Failed to extract ID") // TODO: better exception

            return NyaaTorrent(
                    id,
                    element.getElementsByTag("title").single().text(),
                    extractMagnet(element.getElementsByTag("guid").single().text()),
                    element.getElementsByTag("nyaa:infoHash").single().text(),
                    parseDatetime(element.getElementsByTag("pubDate").single().text()),
                    false
            )
        } catch (e: Exception) {
            throw e // TODO: wrap into another exception
        }
    }

    private fun extractMagnet(viewLink: String): String {
        val document = createConnection(viewLink, false).get()
        document.getElementById("comments").remove()
        return document.select("[href^='magnet:']").single().attr("href")
    }

    private fun parseDatetime(text: String) = datetimeFormat.parse(text).asDateTime
}
