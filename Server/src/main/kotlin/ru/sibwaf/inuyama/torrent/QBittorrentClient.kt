package ru.sibwaf.inuyama.torrent

import org.jsoup.Jsoup
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware

class QBittorrentClient(override val kodein: Kodein) : TorrentClient, KodeinAware {

    // TODO: configuration
    override fun download(magnet: String, directory: String?) {
        Jsoup.connect("http://localhost:9091/command/download")
                .data("urls", magnet)
                .apply {
                    if (directory != null) {
                        data("savepath", directory)
                    }
                }
                .post()
    }
}
