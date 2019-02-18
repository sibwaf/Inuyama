package ru.dyatel.inuyama.rutracker

import android.content.Context
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.R
import ru.dyatel.inuyama.RemoteService
import ru.dyatel.inuyama.SERVICE_RUTRACKER
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

    override val serviceId = SERVICE_RUTRACKER

    private val configuration by instance<RutrackerConfiguration>()
    override val networkManager by instance<NetworkManager>()

    override fun getName(context: Context) = context.getString(R.string.module_rutracker)!!

    override fun checkConnection(): Boolean {
        return try {
            createConnection(configuration.host).get()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun extractMagnet(topic: Long): String {
        try {
            val links = createConnection(generateLink(topic)).get()
                    .select("a[data-topic_id=$topic]")
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