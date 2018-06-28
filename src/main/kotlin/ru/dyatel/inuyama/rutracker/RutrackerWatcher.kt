package ru.dyatel.inuyama.rutracker

import io.objectbox.Box
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.MagnetParser
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.RutrackerWatch_
import ru.dyatel.inuyama.transmission.TorrentClient
import ru.dyatel.inuyama.transmission.TransmissionException

class RutrackerWatcher(override val kodein: Kodein) : KodeinAware, Watcher {

    private val api by instance<RutrackerApi>()
    private val torrentClient by instance<TorrentClient>()
    private val watchBox by instance<Box<RutrackerWatch>>()

    private val undispatchedQuery by lazy {
        watchBox.query()
                .notNull(RutrackerWatch_.magnet)
                .equal(RutrackerWatch_.updateDispatched, false)
                .build()
    }

    override fun checkUpdates(): List<String> {
        return watchBox.all
                .mapNotNull {
                    val magnet = try {
                        api.extractMagnet(it.id)
                    } catch (e: RutrackerException) {
                        // TODO
                        return@mapNotNull null
                    }

                    val oldHash = it.magnet?.let { MagnetParser.extractHash(it) }
                    if (oldHash == null || oldHash != MagnetParser.extractHash(magnet)) {
                        it.magnet = magnet
                        it.lastUpdate = System.currentTimeMillis()
                        it.updateDispatched = false
                        watchBox.put(it)

                        return@mapNotNull it
                    }

                    return@mapNotNull null
                }
                .map { it.description }
    }

    override fun dispatchUpdates() {
        watchBox.store.runInTx {
            undispatchedQuery.find().forEach {
                try {
                    torrentClient.download(it.magnet!!, it.directory.target?.path)

                    it.updateDispatched = true
                    watchBox.put(it)
                } catch (e: TransmissionException) {
                    // TODO
                }
            }
        }
    }

}