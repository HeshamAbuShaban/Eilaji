package dev.anonymous.eilaji.reminder_system.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.anonymous.eilaji.reminder_system.database.db.ReminderDatabase
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = ReminderDatabase.getDatabase(context)
                val list = db.reminderDao().getAllSync().filter { it.isActive }
                val sched = ReminderScheduler(context.applicationContext)
                list.forEach { r ->
                    try {
                        val st = r.scheduleTime ?: "08:00:00"
                        val freq = r.frequency ?: "DAILY"
                        val cd = r.customDays ?: "[]"
                        sched.scheduleExact(r.id, r.medicineName ?: r.text ?: "Medicine", r.notificationId, freq, cd, st)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }
}
