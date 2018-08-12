package ru.dyatel.inuyama.rutracker

import io.objectbox.Box
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.RutrackerWatch_
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.transmission.TorrentClient
import ru.dyatel.inuyama.transmission.TransmissionException
import ru.dyatel.inuyama.utilities.MagnetParser
import ru.dyatel.inuyama.utilities.subscribeFor

class RutrackerWatcher(override val kodein: Kodein) : Watcher(), KodeinAware {

    private val api by instance<RutrackerApi>()
    private val torrentClient by instance<TorrentClient>()
    private val watchBox by instance<Box<RutrackerWatch>>()

    private val networkManager by instance<NetworkManager>()

    private val undispatchedQuery by lazy {
        watchBox.query()
                .notNull(RutrackerWatch_.magnet)
                .equal(RutrackerWatch_.updateDispatched, false)
                .build()
    }

    init {
        watchBox.store.subscribeFor<RutrackerWatch>()
                .onlyChanges()
                .observer { notifyListeners() }
    }

    override fun checkUpdates(): List<String> {
        return watchBox.all
                .filter {
                    val magnet = try {
                        api.extractMagnet(it.topic)
                    } catch (e: RutrackerException) {
                        // TODO
                        return@filter false
                    }

                    val oldHash = it.magnet?.let { MagnetParser.extractHash(it) }
                    if (oldHash == null || oldHash != MagnetParser.extractHash(magnet)) {
                        it.magnet = magnet
                        return@filter true
                    }

                    return@filter false
                }
                .onEach {
                    it.lastUpdate = System.currentTimeMillis()
                    it.updateDispatched = false
                    watchBox.put(it)
                }
                .map { it.description }
    }

    override fun dispatchUpdates() {
        if (!networkManager.isNetworkTrusted()) {
            return
        }

        watchBox.store.runInTx {
            for (watch in undispatchedQuery.find()) {
                try {
                    torrentClient.download(watch.magnet!!, watch.directory.target?.path)

                    watch.updateDispatched = true
                    watchBox.put(watch)
                } catch (e: TransmissionException) {
                    // TODO
                }
            }
        }
    }

    override fun listUpdates(): List<Update> {
        return watchBox.all.mapNotNull { update -> update.lastUpdate?.let { Update(update.description, it) } }
    }

}