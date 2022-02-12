package ru.dyatel.inuyama.rutracker

import com.google.gson.Gson
import io.objectbox.Box
import ru.dyatel.inuyama.model.RutrackerWatch
import ru.dyatel.inuyama.utilities.PreferenceHelper
import ru.dyatel.inuyama.utilities.fromJson
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler

class RutrackerBackupHandler(
    private val preferenceHelper: PreferenceHelper,
    private val watchRepository: Box<RutrackerWatch>,

    private val gson: Gson
) : ModuleBackupHandler("sibwaf.rutracker") {
    override fun provideData(): String {
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

        return gson.toJson(data)
    }

    override fun restoreData(data: String) {
        val backup = gson.fromJson<BackupData>(data)

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
