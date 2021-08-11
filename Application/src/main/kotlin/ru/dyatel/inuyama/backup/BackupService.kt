package ru.dyatel.inuyama.backup

import android.util.Log
import ru.dyatel.inuyama.WORK_NAME_BACKUP
import ru.dyatel.inuyama.pairing.PairedApi
import sibwaf.inuyama.app.common.BackgroundService
import sibwaf.inuyama.app.common.backup.BackupProvider

class BackupService(
    private val pairedApi: PairedApi,
    private val backupProviders: Collection<BackupProvider>
) : BackgroundService(WORK_NAME_BACKUP) {

    override val period = 60

    override suspend fun execute() {
        for (provider in backupProviders) {
            try {
                val ready = pairedApi.prepareBackup(provider.moduleName)
                if (!ready) {
                    continue
                }

                val data = provider.provideData()
                pairedApi.makeBackup(provider.moduleName, data)
            } catch (e: Exception) {
                Log.e("BackupService", "Failed to create a backup for module ${provider.moduleName}", e)
            }
        }
    }
}
