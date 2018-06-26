package ru.dyatel.inuyama.nyaa

import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import ru.dyatel.inuyama.RemoteService

class NyaaApi(override val kodein: Kodein) : KodeinAware, RemoteService {

    private companion object {
        const val host = "https://nyaa.si"
    }

    override fun checkConnection(): Boolean {
        return try {
            Jsoup.connect(host).get()
            true
        } catch (e: Exception) {
            false
        }
    }
}
