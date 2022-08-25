package ru.dyatel.inuyama.rutracker

import com.google.gson.Gson
import io.objectbox.Box
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.sibwaf.inuyama.common.utilities.Encoding
import ru.sibwaf.inuyama.common.utilities.gson.fromJson
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler
import java.io.ByteArrayInputStream
import java.io.InputStream

class RutrackerBackupHandler(
    private val preferenceHelper: PreferenceHelper,
    private val watchRepository: Box<RutrackerWatch>,

    private val gson: Gson
) : ModuleBackupHandler("sibwaf.rutracker") {

    override fun provideData(): InputStream {
        val data = BackupData(
            rutrackerConfiguration = BackupRutrackerConfiguration(
                host = preferenceHelper.rutracker.host
            ),
            watches = watchRepository.all.map {
                BackupRutrackerWatch(
                    id = it.id.toString(),
                    topic = it.topic,
                    description = it.description,
                    magnet = it.magnet,
                    lastUpdate = it.lastUpdate,
                    updateDispatched = it.updateDispatched
                )
            }
        )

        val result = Encoding.stringToBytes(gson.toJson(data))
        return ByteArrayInputStream(result)
    }

    override fun restoreData(data: InputStream) {
        val backup = data.reader().use { gson.fromJson<BackupData>(it) }

        preferenceHelper.rutracker = RutrackerConfiguration(
            host = backup.rutrackerConfiguration.host
        )

        watchRepository.store.runInTx {
            watchRepository.removeAll()
            for (watch in backup.watches) {
                watchRepository.put(
                    RutrackerWatch(
                        topic = watch.topic,
                        description = watch.description,
                        magnet = watch.magnet,
                        lastUpdate = watch.lastUpdate,
                        updateDispatched = watch.updateDispatched,
                    )
                )
            }
        }
    }
}

private data class BackupData(
    val rutrackerConfiguration: BackupRutrackerConfiguration,
    val watches: Collection<BackupRutrackerWatch>
)

private data class BackupRutrackerConfiguration(
    val host: String
)

private data class BackupRutrackerWatch(
    val id: String,
    val topic: Long,
    val description: String,
    val magnet: String?,
    val lastUpdate: Long?,
    val updateDispatched: Boolean
)
