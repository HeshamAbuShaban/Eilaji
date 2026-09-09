package dev.anonymous.eilaji.ui.other.reminder

import android.widget.TextClock
import android.widget.TimePicker
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import java.time.LocalTime
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ReminderViewModel : ViewModel() {
    private val _reminderScheduler = MutableLiveData<ReminderScheduler>()
    val reminderScheduler: LiveData<ReminderScheduler> = _reminderScheduler
    fun setReminderScheduler(r: ReminderScheduler) { _reminderScheduler.value = r }
    private val _databaseViewModel = MutableLiveData<ReminderDatabaseViewModel>()
    private val databaseViewModel: LiveData<ReminderDatabaseViewModel> = _databaseViewModel
    fun setDatabaseViewModel(v: ReminderDatabaseViewModel) { _databaseViewModel.value = v }

    fun determinedTheBackGround(binding: FragmentReminderBinding) {
        binding.root.setBackgroundResource(android.R.color.transparent)
    }

    fun buildScheduleTime(binding: FragmentReminderBinding): String {
        val tp = binding.reminderTimePicker
        val h = tp.hour; val m = tp.minute
        return ReminderTimeUtils.formatScheduleTime(LocalTime.of(h, m, 0))
    }

    fun calculateDelay(binding: FragmentReminderBinding): Long {
        val tp = binding.reminderTimePicker
        val lt = LocalTime.of(tp.hour, tp.minute, 0)
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        var cand = now.withHour(lt.hour).withMinute(lt.minute).withSecond(0).withNano(0)
        if (!cand.isAfter(now)) cand = cand.plusDays(1)
        return TimeUnit.MILLISECONDS.toMinutes(cand.toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(1)
    }

    fun showRemainingTime(binding: FragmentReminderBinding) {
        val tp: TimePicker = binding.reminderTimePicker
        val tv: TextClock = binding.remainingTimeTextView
        val lt = LocalTime.of(tp.hour, tp.minute, 0)
        val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        var cand = now.withHour(lt.hour).withMinute(lt.minute).withSecond(0).withNano(0)
        if (!cand.isAfter(now)) cand = cand.plusDays(1)
        val rem = cand.toInstant().toEpochMilli() - System.currentTimeMillis()
        val h = TimeUnit.MILLISECONDS.toHours(rem); val mm = TimeUnit.MILLISECONDS.toMinutes(rem) % 60
        tv.text = String.format("%02d:%02d", h, mm)
    }

    fun clearInputs(binding: FragmentReminderBinding) { with(binding) { reminderNameEditText.text?.clear(); reminderTimePicker.clearFocus() } }
    fun randomUUIDString(): String = UUID.randomUUID().toString()
    fun storeReminderIntoDatabase(r: Reminder) { databaseViewModel.value?.insertReminder(r) }
}
