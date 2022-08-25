package ru.dyatel.inuyama.nyaa

import com.google.gson.Gson
import hirondelle.date4j.DateTime
import io.objectbox.Box
import ru.dyatel.inuyama.model.NyaaTorrent
import ru.dyatel.inuyama.model.NyaaWatch
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler
import java.io.ByteArrayInputStream
import java.io.InputStream

class NyaaBackupHandler(
    private val watchRepository: Box<NyaaWatch>,
    private val torrentRepository: Box<NyaaTorrent>,

    private val gson: Gson
) : ModuleBackupHandler("sibwaf.nyaa") {

    private companion object {
        const val DATE_FORMAT = "YYYY-MM-DD"
        const val DATETIME_FORMAT = "YYYY-MM-DDThh:mm:ss"
    }

    override fun provideData(): InputStream {
        val data = BackupData(
            watches = watchRepository.all.map { watch ->
                BackupWatch(
                    id = watch.id.toString(),
                    description = watch.description,
                    query = watch.query,
                    startDatetime = watch.startDatetime.format(DATE_FORMAT),
                    collectPath = watch.collectPath,
                    lastUpdate = watch.lastUpdate,
                    directoryId = watch.directory.takeUnless { it.isNull }?.targetId.toString(),
                    torrents = watch.torrents.map { torrent ->
                        BackupTorrent(
                            id = torrent.id.toString(),
                            title = torrent.title,
                            hash = torrent.hash,
                            updateDatetime = torrent.updateDatetime.format(DATETIME_FORMAT),
                            dispatched = torrent.dispatched
                        )
                    }
                )
            }
        )

        val result = Encoding.stringToBytes(gson.toJson(data))
        return ByteArrayInputStream(result)
    }

    override fun restoreData(data: InputStream) {
        val backup = data.reader().use { gson.fromJson<BackupData>(it) }

        watchRepository.store.runInTx {
            torrentRepository.removeAll()
            watchRepository.removeAll()
            for (watch in backup.watches) {
                val torrents = watch.torrents.map { torrent ->
                    NyaaTorrent(
                        id = torrent.id.toLong(),
                        title = torrent.title,
                        hash = torrent.hash,
                        updateDatetime = DateTime(torrent.updateDatetime),
                        dispatched = torrent.dispatched
                    ).also {
                        torrentRepository.attach(it)
                    }
                }

                watchRepository.put(
                    NyaaWatch(
                        description = watch.description,
                        query = watch.query,
                        startDatetime = DateTime(watch.startDatetime),
                        collectPath = watch.collectPath,
                        lastUpdate = watch.lastUpdate
                    ).also {
                        watchRepository.attach(it)
                        it.torrents.addAll(torrents)
                    }
                )
            }
        }
    }
}

private data class BackupData(
    val watches: Collection<BackupWatch>
)

private data class BackupWatch(
    val id: String,
    val description: String,
    val query: String,

    val startDatetime: String,

    val collectPath: String,
    val lastUpdate: Long?,

    val directoryId: String?,

    val torrents: Collection<BackupTorrent>
)

private data class BackupTorrent(
    val id: String,
    val title: String,
    val hash: String,

    val updateDatetime: String,

    val dispatched: Boolean
)
