package ru.sibwaf.inuyama.torrent

import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import ru.sibwaf.inuyama.InuyamaConfiguration

val torrentModule = Kodein.Module("torrent") {
    bind<TorrentClient>() with singleton {
        val configuration = instance<InuyamaConfiguration>().torrent

        when (configuration?.type) {
            TorrentClientType.TRANSMISSION -> TransmissionClient(
                configuration = configuration,
                httpClient = instance(),
                gson = instance()
            )
            TorrentClientType.Q_BITTORRENT -> QBittorrentClient(
                configuration = configuration,
                httpClient = instance()
            )
            null -> TorrentClientStub()
        }
    }

    bind<TorrentHttpHandler>() with singleton {
        TorrentHttpHandler(instance())
    }
}
