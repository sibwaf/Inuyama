package ru.dyatel.inuyama.overseer

import androidx.work.Worker
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.Notifier
import ru.dyatel.inuyama.rutracker.RutrackerWatcher

class OverseerWorker : Worker(), KodeinAware {

    override val kodein by closestKodein { applicationContext }

    private val networkManager by instance<NetworkManager>()
    private val notifier by instance<Notifier>()

    private val rutrackerWatcher by instance<RutrackerWatcher>()

    override fun doWork(): Result {
        rutrackerWatcher.checkUpdates()
                .map { it.description }
                .takeUnless { it.isEmpty() }
                ?.let { notifier.notifyUpdates(it) }

        if (networkManager.isNetworkTrusted()) {
            rutrackerWatcher.dispatchUpdates()
        }

        return Result.SUCCESS
    }

}