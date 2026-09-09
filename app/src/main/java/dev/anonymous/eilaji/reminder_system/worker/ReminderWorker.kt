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
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.storage.enums.SoundNumbers
import dev.anonymous.eilaji.ui.main.MainActivity

class ReminderWorker(private val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private var mediaPlayer: MediaPlayer? = null
    override fun doWork(): Result {
        val nid = inputData.getString(ReminderScheduler.KEY_REMINDER_NotificationId)
        val text = inputData.getString(ReminderScheduler.KEY_REMINDER_TEXT)
        val sound = inputData.getInt(ReminderScheduler.KEY_REMINDER_SOUND, SoundNumbers.SoundLong.soundNumber)
        val map = mapOf(SoundNumbers.SoundLong.soundNumber to R.raw.long_reminder, SoundNumbers.SoundBell.soundNumber to R.raw.bell_reminder, SoundNumbers.SoundTalking.soundNumber to R.raw.talking_reminder, SoundNumbers.SoundNice.soundNumber to R.raw.cool_reminder, SoundNumbers.SoundNotify.soundNumber to R.raw.notify_reminder)
        map[sound]?.let { try { mediaPlayer = MediaPlayer.create(context, it); mediaPlayer?.start() } catch (_: Exception) {} }
        if (!text.isNullOrEmpty() && !nid.isNullOrEmpty()) sendNotification(text, nid) else sendDefault()
        return Result.success()
    }
    override fun onStopped() { try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}; mediaPlayer = null; super.onStopped() }
    private fun sendDefault() { sendNotification(context.getString(R.string.reminder_notification), "1") }
    private fun sendNotification(text: String, nid: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK; putExtra(NOTIFICATION_ID, nid) }
        val bitmap = applicationContext.vectorToBitmap(R.drawable.ic_medication)
        val pi = PendingIntent.getActivity(applicationContext, nid.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL).setLargeIcon(bitmap).setSmallIcon(R.drawable.ic_notifications).setContentTitle(applicationContext.getString(R.string.title_reminder_notification)).setContentText(text).setDefaults(NotificationCompat.DEFAULT_ALL).setContentIntent(pi).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_MAX)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audio = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val ch = NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH).apply { enableLights(true); lightColor = Color.RED; enableVibration(true); vibrationPattern = longArrayOf(100,200,300,400,500,400,300,200,400); setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audio); description = "Medication reminders"; name = "Reminders Channel" }
            nm.createNotificationChannel(ch)
        }
        nm.notify(nid.hashCode(), builder.build())
    }
    private fun Context.vectorToBitmap(id: Int): Bitmap? {
        val d = AppCompatResources.getDrawable(this, id) ?: return null
        val b = Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888) ?: return null
        val c = Canvas(b); d.setBounds(0,0,c.width,c.height); d.draw(c); return b
    }
    companion object { const val NOTIFICATION_ID = "eilaji_reminder_notification_id"; const val NOTIFICATION_NAME = "eilaji_reminder"; const val NOTIFICATION_CHANNEL = "eilaji_channel_01" }
}
