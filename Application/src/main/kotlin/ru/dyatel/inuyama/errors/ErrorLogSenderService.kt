package ru.dyatel.inuyama.errors

import ru.dyatel.inuyama.WORK_NAME_ERROR_SENDER
import ru.dyatel.inuyama.pairing.PairedApi
import sibwaf.inuyama.app.common.BackgroundService

class ErrorLogSenderService(
    private val pairedApi: PairedApi,
    private val errorLogManager: ErrorLogManager,
) : BackgroundService(WORK_NAME_ERROR_SENDER) {

    override val period = 60

    override suspend fun execute() {
        while (true) {
            val oldestEntry = errorLogManager.getOldestEntry() ?: break

            pairedApi.logError(oldestEntry)
            errorLogManager.removeEntry(oldestEntry.guid)
        }
    }
}
