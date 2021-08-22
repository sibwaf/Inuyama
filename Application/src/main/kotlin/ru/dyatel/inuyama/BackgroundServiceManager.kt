package ru.dyatel.inuyama

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.kodein.di.android.closestKodein
import org.kodein.di.direct
import org.kodein.di.generic.allInstances
import ru.dyatel.inuyama.utilities.debugOnly
import ru.dyatel.inuyama.utilities.releaseOnly
import sibwaf.inuyama.app.common.BackgroundService
import java.util.concurrent.TimeUnit

class BackgroundServiceManager : BroadcastReceiver() {

    private companion object {

        private fun Context.extractServices(): Collection<BackgroundService> {
            val kodein by closestKodein(this)
            return kodein.direct.allInstances()
        }
    }

    class ServiceWorker(
        private val context: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result {
            val service = context.extractServices().single { it.name in tags }

            return try {
                service.execute()
                Result.success()
            } catch (e: Exception) {
                Log.e("ServiceWorker", "Periodic service work for ${service.name} failed", e)
                Result.failure()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        releaseOnly {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED -> {
                    for (service in context.extractServices()) {
                        context.createJob(service, replacing = true)
                    }
                }
            }
        }
    }

    fun onApplicationStart(context: Context) {
        releaseOnly {
            for (service in context.extractServices()) {
                context.createJob(service, replacing = false)
            }
        }
    }

    fun onActivityStart(context: Context) {
        for (service in context.extractServices()) {
            context.createJob(service, replacing = BuildConfig.DEBUG)
        }
    }

    fun onActivityStop(context: Context) {
        debugOnly {
            for (service in context.extractServices()) {
                context.removeJob(service)
            }
        }
    }

    private fun Context.createJob(service: BackgroundService, replacing: Boolean) {
        val work = PeriodicWorkRequestBuilder<ServiceWorker>(service.period.toLong(), TimeUnit.MINUTES)
            .addTag(service.name)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            service.name,
            if (replacing) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }

    private fun Context.removeJob(service: BackgroundService) {
        WorkManager.getInstance(this).cancelUniqueWork(service.name)
    }
}
