package dev.anonymous.eilaji.reminder_system.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Color.RED
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.MediaPlayer
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.getDefaultUri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.ui.main.MainActivity

class ReminderWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    private var _mediaPlayer: MediaPlayer? = null
    private val  mediaPlayer get() = _mediaPlayer!!

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun doWork(): Result {
        val notificationId = inputData.getString(ReminderScheduler.KEY_REMINDER_NotificationId)
//        val reminderId = inputData.getString(ReminderScheduler.KEY_REMINDER_ID)
        val reminderText = inputData.getString(ReminderScheduler.KEY_REMINDER_TEXT)

        /**
      * TODO: val databaseID_forNotificationToKillItWhenDelete
      * = inputData.getInt(ReminderScheduler.KEY_REMINDER_TEXT)
      */

        if (!reminderText.isNullOrEmpty() || !notificationId.isNullOrEmpty()) {
            // Show Notification with the user text
            sendNotification(reminderText!!,notificationId!!)

            Log.d("ReminderWorker", "doWork() called In If Statement @Before")

            // Create a MediaPlayer object
            _mediaPlayer = MediaPlayer.create(context, R.raw.long_reminder)
            // set looping
            //mediaPlayer.isLooping = true
            // Play the sound
            mediaPlayer.start()

            // MediaPlayer.create(context, R.raw.notify).start()
            Log.d("ReminderWorker", "doWork() called In If Statement @After $reminderText")
        } else {
            // if the User didn't
            sendNotification("Take Your Pills That You Added!","1")
            /**
             * // Create a MediaPlayer object
             * _mediaPlayer = MediaPlayer.create(context, R.raw.talking_reminder)
             * // set looping
             * mediaPlayer.isLooping = true
             * // OnCompletion Listener ..
             *  /*mediaPlayer.setOnCompletionListener {}*/
             * // Play the sound
             * mediaPlayer.start()
             *
             */

             // Create a MediaPlayer object
             _mediaPlayer = MediaPlayer.create(context, R.raw.talking_reminder)
             // set looping
             //mediaPlayer.isLooping = true
             // OnCompletion Listener ..
              /*mediaPlayer.setOnCompletionListener {}*/
             // Play the sound
             mediaPlayer.start()

        }

        /*Log.d("ReminderWorker", "doWork() called")*/

        return Result.success()
    }

    // lifecycle to kill the mediaPlayer object
    override fun onStopped() {
        super.onStopped()
        mediaPlayer.stop()
        mediaPlayer.release()
        _mediaPlayer = null
    }

    // A Template of Notification to be Built and shown
    private fun sendNotification(reminderText: String , notificationId: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(NOTIFICATION_ID, notificationId)

        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bitmap = applicationContext.vectorToBitmap(R.drawable.ic_medication)
        val titleNotification = applicationContext.getString(R.string.title_reminder_notification)
        val pendingIntent = getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setLargeIcon(bitmap).setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(titleNotification).setContentText(reminderText).setDefaults(DEFAULT_ALL)
            .setContentIntent(pendingIntent).setAutoCancel(true)

        notification.priority = PRIORITY_MAX

        if (SDK_INT >= O) {
            notification.setChannelId(NOTIFICATION_CHANNEL)

            val ringtoneManager = getDefaultUri(TYPE_NOTIFICATION)
            val audioAttributes =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(CONTENT_TYPE_SONIFICATION).build()

            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_NAME, IMPORTANCE_HIGH)

            channel.enableLights(true)
            channel.lightColor = RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            channel.setSound(ringtoneManager, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId.toInt(), notification.build())
    }

    // helper fun that draws the notification
    private fun Context.vectorToBitmap(drawableId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(this, drawableId) ?: return null
        val bitmap = createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        ) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // static values
    companion object {
        const val NOTIFICATION_ID = "eilaji_reminder_notification_id"
        const val NOTIFICATION_NAME = "eilaji_reminder"
        const val NOTIFICATION_CHANNEL = "eilaji_channel_01"
//        const val NOTIFICATION_WORK = "eilaji_notification_work"
    }
}