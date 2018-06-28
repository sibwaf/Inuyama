package ru.dyatel.inuyama.overseer

import androidx.work.Worker
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.allInstances
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.NetworkManager
import ru.dyatel.inuyama.Notifier
import ru.dyatel.inuyama.Watcher

class OverseerWorker : Worker(), KodeinAware {

    override val kodein by closestKodein { applicationContext }

    private val networkManager by instance<NetworkManager>()
    private val notifier by instance<Notifier>()

    private val watchers by allInstances<Watcher>()

    override fun doWork(): Result {
        watchers.map { it.checkUpdates() }
                .flatten()
                .takeUnless { it.isEmpty() }
                ?.let { notifier.notifyUpdates(it) }

        if (networkManager.isNetworkTrusted()) {
            watchers.forEach { it.dispatchUpdates() }
        }

        return Result.SUCCESS
    }

}