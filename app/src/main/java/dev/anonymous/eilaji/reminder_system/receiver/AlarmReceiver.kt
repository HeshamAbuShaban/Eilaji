package dev.anonymous.eilaji.reminder_system.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.main.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("reminder_id") ?: return
        val text = intent.getStringExtra("reminder_text") ?: context.getString(R.string.reminder_notification)
        val nid = intent.getIntExtra("notification_id", 0)
        if (intent.getBooleanExtra("snooze_only", false)) {
            fireAlarm(context, id, text, nid, intent.getStringExtra("dosage"))
            return
        }
        val freq = intent.getStringExtra("frequency") ?: "DAILY"
        val custom = intent.getStringExtra("customDays") ?: "[]"
        val scheduleTime = intent.getStringExtra("scheduleTime") ?: "08:00:00"
        val dosage = lookupDosage(context, id)
        fireAlarm(context, id, text, nid, dosage)
        if (freq.equals("DAILY", true) || freq.equals("WEEKLY", true) || freq.equals("CUSTOM", true)) {
            val days = try { dev.anonymous.eilaji.reminder_system.database.Converters.toList(custom) } catch (_: Exception) { emptyList() }
            if (freq.equals("CUSTOM", true) && days.isNotEmpty()) {
                val todayOk = ReminderTimeUtils.isActiveToday(freq, days)
                if (!todayOk) { reschedule(context, id, text, nid, 0, freq, custom, scheduleTime); return }
            }
            reschedule(context, id, text, nid, 0, freq, custom, scheduleTime)
        }
    }

    private fun lookupDosage(context: Context, id: String): String? {
        return try {
            var out: String? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            Thread {
                try {
                    val db = dev.anonymous.eilaji.reminder_system.database.db.ReminderDatabase.getDatabase(context)
                    out = db.reminderDao().getByIdSync(id)?.getDosage()
                } catch (_: Exception) {}
                latch.countDown()
            }.start()
            latch.await(1200, java.util.concurrent.TimeUnit.MILLISECONDS)
            out
        } catch (_: Exception) { null }
    }

    private fun fireAlarm(context: Context, id: String, text: String, nid: Int, dosage: String?) {
        // Full-screen alarm first; notification is the fallback, not the alarm itself.
        try {
            val full = android.content.Intent(context, dev.anonymous.eilaji.reminder_system.alarm.AlarmActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminder_id", id); putExtra("reminder_text", text)
                putExtra("notification_id", nid); putExtra("dosage", dosage)
            }
            val fullPi = PendingIntent.getActivity(context, nid, full, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            showNotification(context, text, nid, fullPi)
            try { fullPi.send() } catch (_: Exception) {
                try { context.startActivity(full) } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            showNotification(context, text, nid, null)
        }
    }
    private fun reschedule(c: Context, id: String, text: String, nid: Int, sound: Int, freq: String, custom: String, st: String) {
        try { ReminderScheduler(c).rescheduleExact(id, text, nid, sound, freq, custom, st) } catch (_: Exception) {}
    }
    private fun showNotification(c: Context, text: String, nid: Int, fullScreen: PendingIntent?) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = "eilaji_channel_01"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chObj = NotificationChannel(ch, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true); enableVibration(true); vibrationPattern = longArrayOf(100,200,300,400,500,400,300,200,400)
                val aa = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), aa)
                description = "Medication reminders"
            }
            nm.createNotificationChannel(chObj)
        }
        val intent = Intent(c, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pi = PendingIntent.getActivity(c, nid, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(c, ch).setSmallIcon(R.drawable.ic_notifications).setContentTitle(c.getString(R.string.title_reminder_notification)).setContentText(text).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pi)
        if (fullScreen != null) builder.setFullScreenIntent(fullScreen, true)
        nm.notify(nid, builder.build())
        // No custom music: system default tone via channel + vibration pattern + full-screen UI.
    }
}
