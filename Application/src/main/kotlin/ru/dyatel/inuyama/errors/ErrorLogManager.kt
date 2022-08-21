package ru.dyatel.inuyama.errors

import hirondelle.date4j.DateTime
import ru.sibwaf.inuyama.common.paired.model.ErrorLogEntry
import ru.dyatel.inuyama.utilities.PreferenceHelper
import java.util.TimeZone
import java.util.UUID

class ErrorLogManager(
    private val preferenceHelper: PreferenceHelper
) {

    private val lock = Any()

    fun setupExceptionHandler() {
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val entry = ErrorLogEntry(
                timestamp = DateTime.now(TimeZone.getDefault()),
                guid = UUID.randomUUID(),
                stacktrace = throwable.stackTraceToString(),
            )

            synchronized(lock) {
                preferenceHelper.errorLog = preferenceHelper.errorLog + entry
            }

            defaultUncaughtExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getOldestEntry(): ErrorLogEntry? {
        return synchronized(lock) {
            preferenceHelper.errorLog.lastOrNull()
        }
    }

    fun removeEntry(guid: UUID) {
        synchronized(lock) {
            val entries = preferenceHelper.errorLog
            preferenceHelper.errorLog = entries.filter { it.guid != guid }
        }
    }
}
