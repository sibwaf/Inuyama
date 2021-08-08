package ru.dyatel.inuyama.overseer

import hirondelle.date4j.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.dyatel.inuyama.Notifier
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.WORK_NAME_OVERSEER
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.utilities.PreferenceHelper
import sibwaf.inuyama.app.common.BackgroundService
import java.util.TimeZone

typealias OverseerListener = suspend (Boolean) -> Unit

class OverseerService(
    private val preferenceHelper: PreferenceHelper,
    private val notifier: Notifier,
    private val updateDispatchExecutor: UpdateDispatchExecutor,
    private val watchers: Collection<Watcher>
) : BackgroundService(WORK_NAME_OVERSEER) {

    override val period get() = preferenceHelper.overseer.period

    private val listeners = mutableListOf<OverseerListener>()

    var isWorking = false
        private set

    override suspend fun execute() {
        if (isWorking) {
            return // todo: replace job
        }

        try {
            withContext(Dispatchers.Main) {
                isWorking = true
                listeners.forEach { it(isWorking) }
            }

            // todo: set last check after check, not after everything?
            watchers.flatMap { it.checkUpdates() }
                .takeUnless { it.isEmpty() }
                ?.let { notifier.notifyUpdates(it) }

            val dispatcher = UpdateDispatcher()
            for (watcher in watchers) {
                watcher.dispatchUpdates(dispatcher)
            }
            dispatcher.dispatchOn(updateDispatchExecutor)
        } finally {
            preferenceHelper.lastCheck = DateTime.now(TimeZone.getDefault())
            withContext(Dispatchers.Main) {
                isWorking = false
                listeners.forEach { it(isWorking) }
            }
        }
    }

    fun addListener(listener: OverseerListener): OverseerListener {
        listeners += listener
        return listener
    }

    fun removeListener(listener: OverseerListener) {
        listeners -= listener
    }
}