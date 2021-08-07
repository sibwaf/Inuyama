package ru.sibwaf.inuyama.torrent

import io.javalin.Javalin
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.web.SecurityFilter.Companion.decryptBodyAs
import ru.sibwaf.inuyama.web.WebHandler

class TorrentWebHandler(private val torrentClient: TorrentClient) : WebHandler {

    override fun install(javalin: Javalin) {
        javalin.apply {
            post("/download-torrent") { ctx ->
                val request = ctx.decryptBodyAs<TorrentDownloadApiRequest>()
                torrentClient.download(request.magnet, request.path)
            }
        }
    }
}
