package ru.dyatel.inuyama.overseer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ru.dyatel.inuyama.WORK_NAME_OVERSEER
import java.util.concurrent.TimeUnit

class OverseerStarter : BroadcastReceiver() {

    companion object {

        fun start(context: Context, replacing: Boolean) {
            val kodein by closestKodein(context)
            val configuration by kodein.instance<OverseerConfiguration>()

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val work = PeriodicWorkRequestBuilder<OverseerWorker>(configuration.period.toLong(), TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

            val policy = if (replacing) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP

            WorkManager.getInstance().enqueueUniquePeriodicWork(WORK_NAME_OVERSEER, policy, work)
        }

    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            start(context, false)
        }
    }

}
