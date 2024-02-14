package ru.dyatel.inuyama.backup

import android.util.Log
import ru.dyatel.inuyama.WORK_NAME_BACKUP
import ru.dyatel.inuyama.pairing.PairedApi
import sibwaf.inuyama.app.common.BackgroundService
import sibwaf.inuyama.app.common.backup.ModuleBackupHandler

class BackupService(
    private val pairedApi: PairedApi,
    private val backupHandlers: Collection<ModuleBackupHandler>
) : BackgroundService(WORK_NAME_BACKUP) {

    override val period = 15

    override suspend fun execute() {
        for (provider in backupHandlers) {
            try {
                pairedApi.makeBackup(provider.moduleName, provider::provideData)
            } catch (e: Exception) {
                Log.e("BackupService", "Failed to create a backup for module ${provider.moduleName}", e)
            }
        }
    }

    suspend fun restoreEverything() {
        for (handler in backupHandlers) {
            try {
                restoreModule(handler.moduleName)
            } catch (e: Exception) {
                Log.e("BackupService", "Failed to restore backup for module ${handler.moduleName}", e)
            }
        }
    }

    suspend fun restoreModule(name: String) {
        val handler = backupHandlers.firstOrNull { it.moduleName == name }
            ?: throw RuntimeException("No backup handler for module $name")

        val data = pairedApi.getBackupContent(name)
        handler.restoreData(data)
    }
}
