package ru.dyatel.inuyama.overseer

import androidx.work.Worker
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class OverseerWorker : Worker(), KodeinAware {

    override val kodein by closestKodein { applicationContext }

    override fun doWork(): Result {
        return Result.SUCCESS
    }

}