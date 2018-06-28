package ru.dyatel.inuyama.nyaa

import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.asDateTime
import ru.dyatel.inuyama.model.NyaaTorrent
import java.text.SimpleDateFormat
import java.util.Locale

class NyaaApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private companion object {
        const val host = "https://nyaa.si"
        val idPattern = Regex("/(\\d+)$")
        val datetimeFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }

    override fun getName(context: Context) = context.getString(R.string.module_nyaa)!!

    override fun checkConnection(): Boolean {
        return try {
            Jsoup.connect(host).get()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun query(query: String): List<NyaaTorrent> {
        try {
            return Jsoup.connect(host)
                    .data("page", "rss").data("f", "0").data("c", "0_0").data("q", query)
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
                    element.getElementsByTag("link").single().text(),
                    element.getElementsByTag("nyaa:infoHash").single().text(),
                    parseDatetime(element.getElementsByTag("pubDate").single().text()),
                    false
            )
        } catch (e: Exception) {
            throw e // TODO: wrap into another exception
        }
    }

    private fun parseDatetime(text: String) = datetimeFormat.parse(text).asDateTime
}
