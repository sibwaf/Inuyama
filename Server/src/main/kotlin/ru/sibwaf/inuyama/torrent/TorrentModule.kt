package ru.sibwaf.inuyama.torrent

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

val torrentModule = Kodein.Module("torrent") {
    bind<TorrentClient>() with singleton { QBittorrentClient(kodein) }

    bind<TorrentWebHandler>() with singleton {
        TorrentWebHandler(instance())
    }
}
