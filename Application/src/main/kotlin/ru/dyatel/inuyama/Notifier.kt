package ru.dyatel.inuyama

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import org.jetbrains.anko.notificationManager

class Notifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = arrayListOf(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_UPDATES,
                    context.getString(R.string.notification_channel_updates),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            context.notificationManager.createNotificationChannels(channels)
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

        context.notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
    }
}
