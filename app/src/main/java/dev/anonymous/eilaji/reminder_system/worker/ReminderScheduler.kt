package dev.anonymous.eilaji.reminder_system.worker

import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.storage.enums.SoundNumbers
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private lateinit var reminder:Reminder
    private var soundNumber: Int = SoundNumbers.SoundLong.soundNumber

    // Must be Called first !
    fun setReminderObject(reminder: Reminder){
        this.reminder = reminder
    }

    // this to change the media sound for the user
    fun setReminderSound(soundId:Int){
        val matchingEnum = SoundNumbers.values().find { it.soundNumber == soundId }
        if (matchingEnum != null) {
            this.soundNumber = matchingEnum.soundNumber
        } else {
            // Handle the case where no matching enum is found for the given soundId
            Toast.makeText(context,"Enter a valid sound id",Toast.LENGTH_LONG).show()
        }
    }



    // This For one Time Reminder
    fun scheduleReminderOneTimeWorkRequest(timeUnit: TimeUnit = TimeUnit.MINUTES) {
        val inputData = Data.Builder()
            .putString(KEY_REMINDER_ID, reminder.id)
            .putString(KEY_REMINDER_TEXT, reminder.text)
            .putString(KEY_REMINDER_NotificationId, reminder.notificationId.toString())
            .putInt(KEY_REMINDER_SOUND,soundNumber)
            .build()

        /*val constraints = Constraints.Builder()
            *//*.setRequiredNetworkType(NetworkType.CONNECTED)*//*
            .build()*/

        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .addTag(reminder.id) //.. this is important in order to cancel the reminder
            .setInitialDelay(reminder.delayedTime.toLong(), timeUnit)
            /*.setConstraints(constraints)*/
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(reminderRequest)
        Toast.makeText(context,"TheWorkManager has Enqueued Your Request ✔",Toast.LENGTH_SHORT).show()
    }

    // These for The Repeated Processes !
    fun scheduleReminderPeriodicWorkRequest(repeatInterval: Long = 1, timeUnit: TimeUnit = TimeUnit.MINUTES) {
        val inputData = Data.Builder()
            .putString(KEY_REMINDER_NotificationId, reminder.notificationId.toString())
            .putString(KEY_REMINDER_ID, reminder.id)
            .putString(KEY_REMINDER_TEXT, reminder.text)
            .putInt(KEY_REMINDER_SOUND,soundNumber)
            .build()

        /*val constraints = Constraints.Builder()
            *//*.setRequiredNetworkType(NetworkType.CONNECTED)*//*
            .build()*/

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(repeatInterval, timeUnit)
            .addTag(reminder.id) //.. this is important in order to cancel the reminder
            .setInitialDelay(reminder.delayedTime.toLong(), timeUnit)// at first i forgot to put this line that make a delay
            /*.setConstraints(constraints)*/
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(reminderRequest)
    }

    // this must be called after a Query Result
    fun cancelReminderById(reminder: Reminder) {
        // this is how the id looks like: eilaji_reminder_33101bf3-93f5-4c09-9a38-f9545d2085da
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(reminder.id)
        // cancel it notification
        cancelNotification(reminder.notificationId)
    }

    // accessit with the cancellation of the worker
    private fun cancelNotification(notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    //fetch a workInfo by it reminder id which it meant to be sat as the tag
    fun fetchWorkInfoByTag(reminder: Reminder) : LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosByTagLiveData(reminder.id)
    }

    companion object {
        const val KEY_REMINDER_ID = "reminderID"
        const val KEY_REMINDER_TEXT = "reminderText"
        const val KEY_REMINDER_NotificationId = "reminderNotificationId"
        const val KEY_REMINDER_SOUND = "reminderSound"
    }
}
