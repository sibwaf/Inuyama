package ru.dyatel.inuyama.nyaa

import io.objectbox.Box
import io.objectbox.BoxStore
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaTorrent_
import ru.dyatel.inuyama.model.NyaaWatch
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.utilities.subscribeFor

class NyaaWatcher(override val kodein: Kodein) : Watcher(), KodeinAware {

    private val api by instance<NyaaApi>()

    private val boxStore by instance<BoxStore>()
    private val torrentBox by instance<Box<NyaaTorrent>>()
    private val watchBox by instance<Box<NyaaWatch>>()

    private val undispatchedQuery by lazy {
        torrentBox.query()
                .equal(NyaaTorrent_.dispatched, false)
                .build()
    }

    init {
        boxStore.subscribeFor<NyaaWatch>()
                .onlyChanges()
                .observer { notifyListeners() }
    }

    override fun checkUpdates(): List<String> {
        val updates = mutableListOf<String>()

        boxStore.runInTx {
            for (watch in watchBox.all) {
                val torrents = try {
                    api.query(watch.query)
                } catch (e: Exception) { // TODO: catch proper exception
                    continue
                }

                val updated = torrents
                        .filter {
                            val old = watch.torrents.getById(it.id)
                            old == null || old.hash != it.hash
                        }
                        .filter { it.updateDatetime.gteq(watch.startDatetime) }
                        .onEach { watch.torrents.add(it) }
                        .any()

                if (updated) {
                    watch.lastUpdate = System.currentTimeMillis()
                    watchBox.put(watch)

                    updates += watch.description
                }
            }
        }
        return updates
    }

    override fun dispatchUpdates(dispatcher: UpdateDispatcher) {
        for (torrent in undispatchedQuery.find()) {
            val watch = torrent.watch.target
            val directory = watch.directory.target
                    ?.let { "${it.path}/" }
                    ?: ""

            dispatcher.transaction {
                downloadTorrent(torrent.link, directory + watch.collectPath)

                onSuccess {
                    torrent.dispatched = true
                    torrentBox.put(torrent)
                }
            }
        }
    }

    override fun listUpdates(): List<Update> {
        return watchBox.all.mapNotNull { update -> update.lastUpdate?.let { Update(update.description, it) } }
    }

}
