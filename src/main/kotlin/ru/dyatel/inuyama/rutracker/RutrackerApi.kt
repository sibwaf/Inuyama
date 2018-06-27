package ru.dyatel.inuyama.rutracker

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.RemoteService
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI

class RutrackerApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    companion object {

        private const val topicUrl = "/forum/viewtopic.php?t="
        private val pattern = Regex(Regex.escape(topicUrl) + "(\\d+)$")

        fun extractTopic(link: String) =
                pattern.find(link)
                        ?.let { it.groupValues[1].toLong() }
                        ?: throw NotRutrackerLinkException()

    }

    private val configuration by instance<RutrackerConfiguration>()

    private fun Connection.prepare(): Connection {
        configuration.proxy?.let { proxy(it.host, it.port) }
        return this
    }

    override fun checkConnection(): Boolean {
        return try {
            Jsoup.connect(configuration.host).prepare().get()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun extractMagnet(topic: Long): String {
        try {
            val connection = Jsoup.connect(generateLink(topic)).prepare()

            val links = connection.get().select("a[data-topic_id=$topic]")
            if (links.size == 1) {
                return links[0].attr("href")
            }

            throw MagnetRetrievingException("Didn't find an element that contains a magnet link")
        } catch (e: SocketTimeoutException) {
            throw RutrackerTimeoutException()
        } catch (e: IOException) {
            throw RutrackerConnectionException(e)
        }
    }

    fun generateLink(topic: Long) =
            URI("${configuration.host}$topicUrl$topic").normalize().toString()

}
