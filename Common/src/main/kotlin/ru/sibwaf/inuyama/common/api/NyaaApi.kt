package ru.sibwaf.inuyama.common.api

import hirondelle.date4j.DateTime
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ru.sibwaf.inuyama.common.utilities.asDateTime
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import java.text.SimpleDateFormat
import java.util.Locale

data class NyaaTorrent(val id: String, val title: String, val hash: String, val lastUpdate: DateTime)

class NyaaApi(val host: String = "https://nyaa.si") {

    private companion object {
        val idPattern = Regex("/(\\d+)$")
        val datetimeFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
    }

    private fun Response.parseBody(): Document {
        return Parser.xmlParser().parseInput(body!!.string(), "")
    }

    suspend fun query(query: String, httpClient: OkHttpClient): List<NyaaTorrent> {
        fun parse(element: Element): NyaaTorrent {
            val guid = element.getElementsByTag("guid").single().text()
            val id = (idPattern.find(guid) ?: throw Exception("Failed to extract ID")).groupValues[1]

            val datetime = element.getElementsByTag("pubDate").single().text()

            return NyaaTorrent(
                id = id,
                title = element.getElementsByTag("title").single().text(),
                hash = element.getElementsByTag("nyaa:infoHash").single().text(),
                lastUpdate = datetimeFormat.parse(datetime).asDateTime
            )
        }

        val request = Request.Builder()
            .url("$host/?page=rss&f=2&c=0_0&q=$query")
            .build()

        val response = httpClient.newCall(request).await()
        response.use {
            val body = it.successOrThrow().parseBody()
            return body.select("channel > item").map { item -> parse(item) }
        }
    }

    suspend fun getMagnet(torrent: NyaaTorrent, httpClient: OkHttpClient): String {
        return getMagnet(torrent.id, httpClient)
    }

    suspend fun getMagnet(id: String, httpClient: OkHttpClient): String {
        val request = Request.Builder()
            .url("$host/view/$id")
            .build()

        val response = httpClient.newCall(request).await()
        response.use {
            val body = it.successOrThrow().parseBody()
            body.getElementById("comments")?.remove()
            return body.select("[href^='magnet:']").single().attr("href")
        }
    }
}