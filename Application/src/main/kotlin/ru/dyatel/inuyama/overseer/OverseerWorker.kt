package ru.dyatel.inuyama.overseer

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import hirondelle.date4j.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.Notifier
import ru.dyatel.inuyama.UpdateDispatcher
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.utilities.PreferenceHelper
import java.util.TimeZone

typealias OverseerListener = (Boolean) -> Unit

class OverseerWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams), KodeinAware {

    companion object {

        private val listeners = mutableListOf<OverseerListener>()

        var isWorking = false
            private set(value) {
                field = value

                runBlocking(Dispatchers.Main) {
                    listeners.forEach { it(value) }
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

    override val kodein by closestKodein { applicationContext }

    private val preferenceHelper by instance<PreferenceHelper>()

    private val networkManager by instance<NetworkManager>()
    private val notifier by instance<Notifier>()

    private val watchers by allInstances<Watcher>()

    override fun doWork(): Result {
        try {
            isWorking = true

            watchers.map { it.checkUpdates() }
                    .flatten()
                    .takeUnless { it.isEmpty() }
                    ?.let { notifier.notifyUpdates(it) }

            val dispatcher = UpdateDispatcher(kodein)
            watchers.forEach { it.dispatchUpdates(dispatcher) }
            dispatcher.commit()

            return Result.success()
        } catch (e: Exception) {
            Log.e("Overseer", "", e)
            return Result.failure()
        } finally {
            preferenceHelper.lastCheck = DateTime.now(TimeZone.getDefault())
            isWorking = false
        }
    }

}