package ru.sibwaf.inuyama.common.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import ru.sibwaf.inuyama.common.utilities.await
import ru.sibwaf.inuyama.common.utilities.successOrThrow
import java.net.URI

open class RutrackerApi(val host: String = "https://rutracker.org") {

    companion object {

        private const val topicUrl = "/forum/viewtopic.php?t="
        private val pattern = Regex(Regex.escape(topicUrl) + "(\\d+)$")

        fun extractTopic(link: String): Long {
            val match = pattern.find(link) ?: throw IllegalArgumentException("Bad link: $link")
            return match.groupValues[1].toLong()
        }

    }

    private fun Response.parseBody(): Document {
        return Parser.xmlParser().parseInput(body!!.string(), "")
    }

    suspend fun getMagnet(topic: Long, httpClient: OkHttpClient): String {
        val request = Request.Builder()
            .url(generateLink(topic))
            .build()

        val response = httpClient.newCall(request).await()
        response.use {
            val body = it.successOrThrow().parseBody()
            val link = body.select("a[data-topic_id=$topic]").single()
            return link.attr("href")
        }
    }

    fun generateLink(topic: Long): String = URI("$host$topicUrl$topic").normalize().toString()
}