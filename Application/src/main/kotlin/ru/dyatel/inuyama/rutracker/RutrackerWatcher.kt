package ru.dyatel.inuyama.rutracker

import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.RutrackerWatch_
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.utilities.subscribeFor
import ru.sibwaf.inuyama.common.utilities.MagnetParser

class RutrackerWatcher(
    private val api: RutrackerApiService,
    private val boxStore: BoxStore,
    private val watchBox: Box<RutrackerWatch>,
) : Watcher() {

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
        val updates = mutableListOf<String>()

        boxStore.runInTx {
            val data = runBlocking {
                watchBox.all.mapNotNull {
                    try {
                        it to api.getMagnet(it.topic)
                    } catch (e: Exception) { // TODO: catch proper exception
                        null
                    }
                }
            }

            for ((watch, magnet) in data) {
                val oldHash = watch.magnet?.let { MagnetParser.extractHash(it) }
                watch.magnet = magnet

                if (oldHash != MagnetParser.extractHash(magnet)) {
                    watch.lastUpdate = System.currentTimeMillis()
                    watch.updateDispatched = false
                    updates += watch.description
                }

                watchBox.put(watch)
            }
        }
        return updates
    }

    override fun dispatchUpdates(dispatcher: UpdateDispatcher) {
        for (watch in undispatchedQuery.find()) {
            dispatcher.transaction {
                downloadTorrent(watch.magnet!!, watch.directory.target?.path ?: "")

                onSuccess {
                    watch.updateDispatched = true
                    watchBox.put(watch)
                }
            }
        }
    }

    override fun listUpdates(): List<Update> {
        return watchBox.all.mapNotNull { update -> update.lastUpdate?.let { Update(update.description, it) } }
    }

}