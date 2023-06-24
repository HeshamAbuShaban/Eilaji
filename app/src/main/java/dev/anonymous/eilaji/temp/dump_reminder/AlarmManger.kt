package dev.anonymous.eilaji.temp.dump_reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmManger {

    fun scheduleAlarm(context: Context, reminder: MedicationReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationReminderReceiver::class.java)
        intent.putExtra("medicationName", reminder.medicationName)
        // Add any other necessary reminder details as extras

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val reminderTime = reminder.reminderDate.time

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

}
class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationName = intent.getStringExtra("medicationName")
        // Retrieve other reminder details from the intent extras

        // Display notification or perform required actions for the medication reminder
        // You can use NotificationManager to show a notification or start a foreground service
    }
}
