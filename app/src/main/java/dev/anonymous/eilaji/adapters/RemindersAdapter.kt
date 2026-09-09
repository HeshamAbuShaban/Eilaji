package dev.anonymous.eilaji.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemindersAdapter(val remindersList: ArrayList<Reminder>) : RecyclerView.Adapter<RemindersAdapter.RemindersViewHolder>() {
    private lateinit var cb: RemindersListCallback
    fun registerRemindersListCallback(c: RemindersListCallback) { cb = c }
    override fun onCreateViewHolder(p: ViewGroup, v: Int) = RemindersViewHolder(ItemReminderBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = remindersList.size
    override fun onBindViewHolder(h: RemindersViewHolder, pos: Int) { h.bind(remindersList[pos]); h.setCallback(cb) }
    class RemindersViewHolder(private val b: ItemReminderBinding) : RecyclerView.ViewHolder(b.root) {
        private lateinit var cb: RemindersListCallback
        fun setCallback(c: RemindersListCallback) { cb = c }
        @SuppressLint("SetTextI18n")
        fun bind(r: Reminder) {
            with(b) {
                reminderType.setImageResource(if ((r.frequency ?: "DAILY") == "DAILY") R.drawable.ic_repeat else R.drawable.ic_one)
                reminderName.text = r.medicineName ?: r.text ?: "Medicine"
                val dosage = r.dosage?.takeIf { it.isNotBlank() }?.let { "$it • " } ?: ""
                val freq = r.frequency ?: "DAILY"
                val days = if (freq == "CUSTOM") r.getCustomDaysList().joinToString(",") else ""
                val freqLabel = if (freq == "CUSTOM" && days.isNotBlank()) "CUSTOM ($days)" else freq
                reminderDosage.text = "$dosage$freqLabel"
                val st = r.scheduleTime ?: "08:00:00"
                val lt = ReminderTimeUtils.parseScheduleTime(st)
                reminderDelayTime.text = lt?.let { ReminderTimeUtils.formatDisplay(it) } ?: st
                reminderCreationTimestamp.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(r.creationTimestamp))
                switchActive.isChecked = r.isActive()
                switchActive.setOnCheckedChangeListener { _, isChecked ->
                    if (r.isActive() != isChecked) { r.setActive(isChecked); cb.onToggleActive(r, isChecked) }
                }
                deleteReminder.setOnClickListener { cb.onDeleteClicked(r) }
                root.alpha = if (r.isActive()) 1f else 0.55f
            }
        }
    }
    interface RemindersListCallback { fun onDeleteClicked(reminder: Reminder); fun onToggleActive(reminder: Reminder, active: Boolean) }
}
