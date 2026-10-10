package dev.anonymous.eilaji.reminder_system.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.reminder_system.ReminderReliability
import dev.anonymous.eilaji.ui.main.MainActivity

class ReminderWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val nid = inputData.getString(ReminderScheduler.KEY_REMINDER_NotificationId)
        val text = inputData.getString(ReminderScheduler.KEY_REMINDER_TEXT)
        // System default tone via channel only — no custom music.
        if (!text.isNullOrEmpty() && !nid.isNullOrEmpty()) sendNotification(text, nid) else sendDefault()
        return Result.success()
    }
    override fun onStopped() { super.onStopped() }
    private fun sendDefault() { sendNotification(context.getString(R.string.reminder_notification), "1") }
    private fun sendNotification(text: String, nid: String) {
        val app = applicationContext
        ReminderReliability.ensureChannel(app)
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NOTIFICATION_ID, nid)
        }
        val pi = PendingIntent.getActivity(app, nid.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(app, ReminderReliability.CHANNEL_ID)
            .setLargeIcon(app.vectorToBitmap(R.drawable.ic_medication))
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(app.getString(R.string.title_reminder_notification))
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        try { nm.notify(nid.hashCode(), builder.build()) } catch (e: Exception) {
            android.util.Log.e("ReminderWorker", "Could not post reminder", e)
        }
    }
    private fun Context.vectorToBitmap(id: Int): Bitmap? {
        val d = AppCompatResources.getDrawable(this, id) ?: return null
        val b = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888) ?: return null
        val c = Canvas(b); d.setBounds(0,0,c.width,c.height); d.draw(c); return b
    }
    companion object { const val NOTIFICATION_ID = "eilaji_reminder_notification_id"; const val NOTIFICATION_NAME = "eilaji_reminder"; const val NOTIFICATION_CHANNEL = "eilaji_channel_01" }
}
