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
import sibwaf.inuyama.app.common.BackgroundService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class BackgroundServiceManager : BroadcastReceiver() {

    private companion object {
        val serviceRegistry = ConcurrentHashMap<UUID, BackgroundService>()
    }

    class ServiceWorker(
        context: Context,
        workerParams: WorkerParameters
    ) : CoroutineWorker(context, workerParams) {

        override suspend fun doWork(): Result {
            val service = serviceRegistry.getValue(id)
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
        if (BuildConfig.DEBUG) {
            return
        }

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                for (service in context.extractServices()) {
                    context.createJob(service, replacing = false)
                }
            }
        }
    }

    fun onActivityStart(context: Context) {
        for (service in context.extractServices()) {
            context.createJob(service, replacing = BuildConfig.DEBUG)
        }
    }

    fun onActivityStop(context: Context) {
        if (!BuildConfig.DEBUG) {
            return
        }

        for (service in context.extractServices()) {
            context.removeJob(service)
        }
    }

    private fun Context.extractServices(): Collection<BackgroundService> {
        val kodein by closestKodein(this)
        return kodein.direct.allInstances()
    }

    private fun Context.createJob(service: BackgroundService, replacing: Boolean) {
        val work = PeriodicWorkRequestBuilder<ServiceWorker>(service.period.toLong(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            service.name,
            if (replacing) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            work
        )

        serviceRegistry[work.id] = service
    }

    private fun Context.removeJob(service: BackgroundService) {
        WorkManager.getInstance(this).cancelUniqueWork(service.name)
    }
}
