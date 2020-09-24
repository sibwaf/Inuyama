package ru.dyatel.inuyama.rutracker

import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.runBlocking
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.model.RutrackerWatch_
import ru.dyatel.inuyama.model.Update
import ru.dyatel.inuyama.utilities.MagnetParser
import ru.dyatel.inuyama.utilities.subscribeFor

class RutrackerWatcher(override val kodein: Kodein) : Watcher(), KodeinAware {

    private val api by instance<RutrackerApiService>()
    private val boxStore by instance<BoxStore>()
    private val watchBox by instance<Box<RutrackerWatch>>()

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
                if (oldHash == null || oldHash != MagnetParser.extractHash(magnet)) {
                    watch.magnet = magnet
                    continue
                }

                watch.lastUpdate = System.currentTimeMillis()
                watch.updateDispatched = false
                watchBox.put(watch)

                updates += watch.description
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