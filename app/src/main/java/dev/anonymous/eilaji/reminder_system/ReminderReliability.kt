package dev.anonymous.eilaji.reminder_system

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Reminder reliability checks.
 *
 * Medication reminders are the one feature that must never fail silently. This object answers
 * three questions: can we post notifications, can we schedule exact alarms, and is the app
 * exempt from battery optimisation. The UI shows a banner while any of them is off, so the user
 * can fix it in two taps instead of wondering why the alarm never came.
 */
object ReminderReliability {

    data class Status(
        val notificationsAllowed: Boolean,
        val canScheduleExact: Boolean,
        val batteryUnrestricted: Boolean
    ) {
        val isReady: Boolean get() = notificationsAllowed && canScheduleExact && batteryUnrestricted
    }

    fun check(context: Context): Status {
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(android.app.AlarmManager::class.java)
            am?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
        val power = context.getSystemService(PowerManager::class.java)
        val battery = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        return Status(notifications, exactAlarms, battery)
    }

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /** Intent that opens the app's notification-permission screen. */
    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    /** Intent that opens the screen where exact alarms are re-enabled. */
    fun exactAlarmSettingsIntent(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.fromParts("package", context.packageName, null))
        } else {
            null
        }

    /** Intent that asks the user to exempt the app from battery optimisation. */
    fun batteryOptimisationIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.fromParts("package", context.packageName, null))

    const val CHANNEL_ID = "eilaji.reminders"

    /**
     * Creates the reminder channel once. The sound is the alarm stream, not the notification
     * stream, so a dose reminder is audible while the phone is in a pocket.
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            context.getString(dev.anonymous.eilaji.R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(dev.anonymous.eilaji.R.string.channel_reminders_description)
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }
}