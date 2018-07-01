package ru.dyatel.inuyama.overseer

import android.util.Log
import androidx.work.Worker
import hirondelle.date4j.DateTime
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.runBlocking
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.Notifier
import ru.dyatel.inuyama.Watcher
import ru.dyatel.inuyama.utilities.PreferenceHelper
import java.util.TimeZone

typealias OverseerListener = (Boolean) -> Unit

class OverseerWorker : Worker(), KodeinAware {

    companion object {

        private val listeners = mutableListOf<OverseerListener>()

        var isWorking = false
            private set(value) {
                field = value

                runBlocking(UI) {
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

            if (networkManager.isNetworkTrusted()) {
                watchers.forEach { it.dispatchUpdates() }
            }

            return Result.SUCCESS
        } catch (e: Exception) {
            Log.e("Overseer", "", e)
            return Result.FAILURE
        } finally {
            preferenceHelper.lastCheck = DateTime.now(TimeZone.getDefault())
            isWorking = false
        }
    }

}