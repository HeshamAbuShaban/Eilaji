package dev.anonymous.eilaji.reminder_system.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.storage.enums.SoundNumbers
import dev.anonymous.eilaji.ui.main.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("reminder_id") ?: return
        val text = intent.getStringExtra("reminder_text") ?: context.getString(R.string.reminder_notification)
        val nid = intent.getIntExtra("notification_id", 0)
        val sound = intent.getIntExtra("sound", SoundNumbers.SoundLong.soundNumber)
        val freq = intent.getStringExtra("frequency") ?: "DAILY"
        val custom = intent.getStringExtra("customDays") ?: "[]"
        val scheduleTime = intent.getStringExtra("scheduleTime") ?: "08:00:00"
        showNotification(context, text, nid, sound)
        if (freq.equals("DAILY", true) || freq.equals("WEEKLY", true) || freq.equals("CUSTOM", true)) {
            val days = try { dev.anonymous.eilaji.reminder_system.database.Converters.toList(custom) } catch (_: Exception) { emptyList() }
            if (freq.equals("CUSTOM", true) && days.isNotEmpty()) {
                val todayOk = ReminderTimeUtils.isActiveToday(freq, days)
                if (!todayOk) { reschedule(context, id, text, nid, sound, freq, custom, scheduleTime); return }
            }
            reschedule(context, id, text, nid, sound, freq, custom, scheduleTime)
        }
    }
    private fun reschedule(c: Context, id: String, text: String, nid: Int, sound: Int, freq: String, custom: String, st: String) {
        try { ReminderScheduler(c).rescheduleExact(id, text, nid, sound, freq, custom, st) } catch (_: Exception) {}
    }
    private fun showNotification(c: Context, text: String, nid: Int, sound: Int) {
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
        val notif = NotificationCompat.Builder(c, ch).setSmallIcon(R.drawable.ic_notifications).setContentTitle(c.getString(R.string.title_reminder_notification)).setContentText(text).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_MAX).setDefaults(NotificationCompat.DEFAULT_ALL).setContentIntent(pi).build()
        nm.notify(nid, notif)
        val map = mapOf(SoundNumbers.SoundLong.soundNumber to R.raw.long_reminder, SoundNumbers.SoundBell.soundNumber to R.raw.bell_reminder, SoundNumbers.SoundTalking.soundNumber to R.raw.talking_reminder, SoundNumbers.SoundNice.soundNumber to R.raw.cool_reminder, SoundNumbers.SoundNotify.soundNumber to R.raw.notify_reminder)
        map[sound]?.let { try { MediaPlayer.create(c, it)?.apply { start() } } catch (_: Exception) {} }
    }
}
