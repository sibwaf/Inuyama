package ru.sibwaf.inuyama.torrent

import io.javalin.Javalin
import ru.sibwaf.inuyama.common.TorrentDownloadApiRequest
import ru.sibwaf.inuyama.http.HttpHandler
import ru.sibwaf.inuyama.http.SecurityHttpFilter.Companion.decryptBodyAs
import ru.sibwaf.inuyama.http.pairedSubroute

class TorrentHttpHandler(private val torrentClient: TorrentClient) : HttpHandler {

    override fun Javalin.install() {
        pairedSubroute {
            post("/download-torrent") { ctx ->
                val request = ctx.decryptBodyAs<TorrentDownloadApiRequest>()
                torrentClient.download(request.magnet, request.path)
            }
        }
    }
}
