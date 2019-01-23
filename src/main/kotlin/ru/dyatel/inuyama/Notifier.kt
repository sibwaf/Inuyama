package ru.dyatel.inuyama

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.kodein.di.generic.on

class Notifier(override val kodein: Kodein) : KodeinAware {

    private val context by instance<Context>()
    private val notificationManager by on(context).instance<NotificationManager>()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = arrayListOf(
                    NotificationChannel(
                            NOTIFICATION_CHANNEL_UPDATES,
                            context.getString(R.string.notification_channel_updates),
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
            )

            notificationManager.createNotificationChannels(channels)
        }
    }

    fun notifyUpdates(updates: List<String>) {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_updates))
                .setContentText(updates.joinToString(", "))
                .setNumber(updates.size)
                .setSound(null)
                .setLights(context.getColor(R.color.color_primary), 2500, 2500)
                .build()

        notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
    }

}
