package dev.anonymous.eilaji.reminder_system.worker

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.anonymous.eilaji.reminder_system.database.Converters
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.entity.ReminderContract
import dev.anonymous.eilaji.reminder_system.receiver.AlarmReceiver
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import dev.anonymous.eilaji.storage.enums.SoundNumbers
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private lateinit var reminder: ReminderContract
    private var soundNumber: Int = SoundNumbers.SoundLong.soundNumber

    fun setReminderObject(reminder: Reminder) { this.reminder = reminder }
    fun setReminderSound(soundId: Int) {
        SoundNumbers.values().find { it.soundNumber == soundId }?.let { soundNumber = it.soundNumber }
            ?: Toast.makeText(context, "Enter a valid sound id", Toast.LENGTH_LONG).show()
    }

    fun scheduleReminderOneTimeWorkRequest(timeUnit: TimeUnit = TimeUnit.MINUTES) {
        if (!::reminder.isInitialized) return
        val r = reminder as? Reminder
        if (r != null && !r.scheduleTime.isNullOrBlank() && r.isActive) {
            scheduleExact(r.id, r.medicineName ?: r.text, r.notificationId, r.frequency ?: "DAILY", r.customDays ?: "[]", r.scheduleTime!!)
            enqueueFallbackOneTime(r)
        } else {
            val inputData = Data.Builder().putString(KEY_REMINDER_ID, reminder.id).putString(KEY_REMINDER_TEXT, reminder.text).putString(KEY_REMINDER_NotificationId, reminder.notificationId.toString()).putInt(KEY_REMINDER_SOUND, soundNumber).build()
            val req = OneTimeWorkRequestBuilder<ReminderWorker>().addTag(reminder.id).setInitialDelay(reminder.delayedTime.coerceAtLeast(1), timeUnit).setInputData(inputData).build()
            WorkManager.getInstance(context).enqueue(req)
        }
        Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
    }

    fun scheduleReminderPeriodicWorkRequest(repeatInterval: Long = 1, timeUnit: TimeUnit = TimeUnit.MINUTES) {
        if (!::reminder.isInitialized) return
        val r = reminder as? Reminder
        if (r != null && !r.scheduleTime.isNullOrBlank()) {
            scheduleExact(r.id, r.medicineName ?: r.text, r.notificationId, r.frequency ?: "DAILY", r.customDays ?: "[]", r.scheduleTime!!)
            enqueueFallbackPeriodic(r, repeatInterval, timeUnit)
        } else {
            val inputData = Data.Builder().putString(KEY_REMINDER_NotificationId, reminder.notificationId.toString()).putString(KEY_REMINDER_ID, reminder.id).putString(KEY_REMINDER_TEXT, reminder.text).putInt(KEY_REMINDER_SOUND, soundNumber).build()
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(repeatInterval, timeUnit).addTag(reminder.id).setInitialDelay(reminder.delayedTime.coerceAtLeast(1), timeUnit).setInputData(inputData).build()
            WorkManager.getInstance(context).enqueue(req)
        }
    }

    fun scheduleExact(id: String, text: String, nid: Int, frequency: String, customDaysJson: String, scheduleTime: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", id); putExtra("reminder_text", text); putExtra("notification_id", nid); putExtra("sound", soundNumber); putExtra("frequency", frequency); putExtra("customDays", customDaysJson); putExtra("scheduleTime", scheduleTime)
        }
        val pi = PendingIntent.getBroadcast(context, nid, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val freq = frequency.uppercase()
        val days = try { Converters.toList(customDaysJson) } catch (_: Exception) { emptyList() }
        val trigger = ReminderTimeUtils.nextTriggerMillis(scheduleTime, freq, days)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (_: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi) }
    }

    fun rescheduleExact(id: String, text: String, nid: Int, sound: Int, frequency: String, customDaysJson: String, scheduleTime: String) {
        soundNumber = sound
        scheduleExact(id, text, nid, frequency, customDaysJson, scheduleTime)
    }

    private fun enqueueFallbackOneTime(r: Reminder) {
        val delay = runCatching { ReminderTimeUtils.nextTriggerMillis(r.scheduleTime!!, r.frequency ?: "DAILY", Converters.toList(r.customDays ?: "[]")) - System.currentTimeMillis() }.getOrDefault(60000L).coerceAtLeast(1000L)
        val mins = TimeUnit.MILLISECONDS.toMinutes(delay).coerceAtLeast(1)
        val data = Data.Builder().putString(KEY_REMINDER_ID, r.id).putString(KEY_REMINDER_TEXT, r.medicineName ?: r.text).putString(KEY_REMINDER_NotificationId, r.notificationId.toString()).putInt(KEY_REMINDER_SOUND, soundNumber).build()
        val req = OneTimeWorkRequestBuilder<ReminderWorker>().addTag(r.id + "_wm").setInitialDelay(mins, TimeUnit.MINUTES).setInputData(data).build()
        WorkManager.getInstance(context).enqueue(req)
    }

    private fun enqueueFallbackPeriodic(r: Reminder, interval: Long, unit: TimeUnit) {
        val data = Data.Builder().putString(KEY_REMINDER_NotificationId, r.notificationId.toString()).putString(KEY_REMINDER_ID, r.id).putString(KEY_REMINDER_TEXT, r.medicineName ?: r.text).putInt(KEY_REMINDER_SOUND, soundNumber).build()
        val mins = ReminderTimeUtils.parseScheduleTime(r.scheduleTime ?: "08:00:00")?.let { 1440L } ?: 1440L
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(mins, TimeUnit.MINUTES).addTag(r.id + "_wm_periodic").setInputData(data).build()
        WorkManager.getInstance(context).enqueue(req)
    }

    fun cancelReminderById(reminder: Reminder) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reminder.id)
        WorkManager.getInstance(context).cancelAllWorkByTag(reminder.id + "_wm")
        WorkManager.getInstance(context).cancelAllWorkByTag(reminder.id + "_wm_periodic")
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, reminder.notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi); pi.cancel()
        } catch (_: Exception) {}
        cancelNotification(reminder.notificationId)
    }

    private fun cancelNotification(nid: Int) { (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(nid) }
    fun fetchWorkInfoByTag(reminder: Reminder): LiveData<List<WorkInfo>> = WorkManager.getInstance(context).getWorkInfosByTagLiveData(reminder.id)
    companion object { const val KEY_REMINDER_ID = "reminderID"; const val KEY_REMINDER_TEXT = "reminderText"; const val KEY_REMINDER_NotificationId = "reminderNotificationId"; const val KEY_REMINDER_SOUND = "reminderSound" }
}
