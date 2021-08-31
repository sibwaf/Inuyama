package ru.sibwaf.inuyama.torrent

import io.javalin.Javalin
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBodyAs

class TorrentHttpHandler(private val torrentClient: TorrentClient) : HttpHandler {

    override fun install(javalin: Javalin) {
        javalin.apply {
            post("/download-torrent") { ctx ->
                val request = ctx.decryptBodyAs<TorrentDownloadApiRequest>()
                torrentClient.download(request.magnet, request.path)
            }
        }
    }
}
