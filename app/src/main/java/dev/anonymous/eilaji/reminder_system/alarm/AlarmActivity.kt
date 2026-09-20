package dev.anonymous.eilaji.reminder_system.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import dev.anonymous.eilaji.databinding.ActivityAlarmBinding
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen alarm: wakes the screen, pulses vibration in a
 * medicine rhythm (no music), shows what + dosage + big time,
 * Take now / Snooze 10 min. Works on lock screen.
 */
class AlarmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmBinding
    private var vibrator: Vibrator? = null
    @Volatile private var vibrating = false
    private var vibeThread: Thread? = null
    private var ringtone: android.media.Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val text = intent.getStringExtra("reminder_text") ?: "Time for your medicine"
        val dosage = intent.getStringExtra("dosage")
        val nid = intent.getIntExtra("notification_id", 0)
        binding.tvAlarmMedicine.text = text
        binding.tvAlarmDosage.text = dosage ?: ""
        binding.tvAlarmDosage.visibility = if (dosage.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        binding.tvAlarmTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        startPulse()
        startAlertTone()
        binding.buTakeNow.setOnClickListener {
            stopAlert()
            try { (getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).cancel(nid) } catch (_: Exception) {}
            finish()
        }
        binding.buSnooze.setOnClickListener {
            stopAlert()
            try {
                val id = intent.getStringExtra("reminder_id") ?: return@setOnClickListener
                ReminderScheduler(applicationContext).snoozeOnce(id, text, nid, 10)
            } catch (_: Exception) {}
            finish()
        }
    }

    /** Strongest default beep/bell: the system's own alarm tone. No custom music. */
    private fun startAlertTone() {
        try {
            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)?.also {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.isLooping = true
                } catch (_: Exception) {}
                it.play()
            }
        } catch (_: Exception) {}
    }

    private fun stopAlert() {
        stopPulse()
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
    }

    private fun startPulse() {
        vibrating = true
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (_: Exception) { null }
        vibeThread = Thread {
            // Two short pulses + pause, repeating — a "take your pill" rhythm
            while (vibrating) {
                try {
                    pulse(180); Thread.sleep(160)
                    pulse(180); Thread.sleep(900)
                } catch (_: Exception) { break }
            }
        }.also { it.start() }
    }

    private fun pulse(ms: Long) {
        try {
            val v = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {}
    }

    private fun stopPulse() {
        vibrating = false
        try { vibeThread?.interrupt() } catch (_: Exception) {}
        try { vibrator?.cancel() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }
}
