package dev.anonymous.eilaji.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.network.websocket.PushPayload

/**
 * Turns a server push payload into a local notification. Shared by the live socket and the
 * offline fallback worker, so both paths look identical to the user.
 */
object PushNotifications {

    const val CHANNEL_ID = "eilaji.push"
    private const val NOTIFICATION_BASE_ID = 7700

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_updates_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_updates_description)
            enableLights(true)
            enableVibration(true)
            setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, payload: PushPayload) {
        ensureChannel(context)
        val intent = Intent(context, dev.anonymous.eilaji.ui.main.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_ROUTE, payload.route)
            putExtra(EXTRA_TITLE, payload.title)
        }
        val pi = PendingIntent.getActivity(
            context,
            payload.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
        val id = NOTIFICATION_BASE_ID + (payload.id.hashCode() and 0x0FFF)
        try {
            context.getSystemService(NotificationManager::class.java)?.notify(id, builder.build())
        } catch (_: Exception) {
        }
    }

    const val EXTRA_ROUTE = "push_route"
    const val EXTRA_TITLE = "push_title"
}
