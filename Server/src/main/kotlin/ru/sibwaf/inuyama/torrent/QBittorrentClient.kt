package ru.sibwaf.inuyama.torrent

import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import java.net.URLEncoder

class QBittorrentClient(override val kodein: Kodein) : TorrentClient, KodeinAware {

    // TODO: configuration
    override fun download(magnet: String, directory: String?) {
        Jsoup.connect("http://localhost:9091/command/download")
                .apply {
                    val charset = request().postDataCharset()

                    var body = "urls=${URLEncoder.encode(magnet, charset)}"

                    if (directory != null) {
                        body += "&savepath=${URLEncoder.encode(directory, charset).replace("+", "%20")}"
                    }

                    requestBody(body)
                }
                .post()
    }
}
