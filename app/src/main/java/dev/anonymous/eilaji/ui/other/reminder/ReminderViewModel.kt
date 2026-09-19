package dev.anonymous.eilaji.ui.other.reminder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class ReminderViewModel : ViewModel() {
    private val _reminderScheduler = MutableLiveData<ReminderScheduler>()
    val reminderScheduler: LiveData<ReminderScheduler> = _reminderScheduler
    fun setReminderScheduler(r: ReminderScheduler) { _reminderScheduler.value = r }
    private val _databaseViewModel = MutableLiveData<ReminderDatabaseViewModel>()
    private val databaseViewModel: LiveData<ReminderDatabaseViewModel> = _databaseViewModel
    fun setDatabaseViewModel(v: ReminderDatabaseViewModel) { _databaseViewModel.value = v }

    // Creator state (replaces reading widgets directly)
    var hour: Int = 8
    var minute: Int = 30

    fun determinedTheBackGround(binding: dev.anonymous.eilaji.databinding.FragmentReminderBinding) {
        binding.root.setBackgroundResource(android.R.color.transparent)
    }

    fun buildScheduleTime(): String {
        return ReminderTimeUtils.formatScheduleTime(LocalTime.of(hour, minute, 0))
    }

    fun nextDoseText(): String {
        return try {
            val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
            var cand = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            val today = cand.isAfter(now)
            if (!today) cand = cand.plusDays(1)
            val rem = cand.toInstant().toEpochMilli() - System.currentTimeMillis()
            val h = TimeUnit.MILLISECONDS.toHours(rem)
            val mm = TimeUnit.MILLISECONDS.toMinutes(rem) % 60
            val whenDay = if (today) "Today" else "Tomorrow"
            if (h == 0L) "Next dose in ${mm}m · $whenDay" else "Next dose in ${h}h ${mm}m · $whenDay"
        } catch (_: Exception) { "" }
    }

    fun randomUUIDString(): String = UUID.randomUUID().toString()
    fun storeReminderIntoDatabase(r: Reminder) { databaseViewModel.value?.insertReminder(r) }
}
