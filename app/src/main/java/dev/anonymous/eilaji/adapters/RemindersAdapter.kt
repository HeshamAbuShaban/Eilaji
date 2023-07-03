package dev.anonymous.eilaji.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.ItemReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.storage.enums.ReminderType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RemindersAdapter(val remindersList:ArrayList<Reminder>) : RecyclerView.Adapter<RemindersAdapter.RemindersViewHolder>() {
    private lateinit var remindersListCallback:RemindersListCallback

    fun registerRemindersListCallback(context: RemindersListCallback){
        remindersListCallback = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemindersViewHolder {
        val binding =ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RemindersViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return remindersList.size
    }

    override fun onBindViewHolder(holder: RemindersViewHolder, position: Int) {
       val reminder = remindersList[position]
        holder.bind(reminder)
        holder.setRemindersListCallback(remindersListCallback)
    }

    class RemindersViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var remindersListCallback: RemindersListCallback

        fun setRemindersListCallback(remindersListCallback: RemindersListCallback){
            this.remindersListCallback = remindersListCallback
        }

        @SuppressLint("SetTextI18n")
        fun bind(reminder: Reminder) {
            with(binding){
                reminderType.setImageResource(when (reminder.reminderType) {
                        ReminderType.OneTime.reminderType -> R.drawable.ic_one
                        else -> R.drawable.ic_repeat
                    })
                reminderName.text = reminder.text
                reminderDelayTime.text = "delay-time: ${reminder.delayedTime} minutes"
                reminderCreationTimestamp.text = getCreationTimeStamp(reminder.creationTimestamp)
                deleteReminder.setOnClickListener {
                    remindersListCallback.onDeleteClicked(reminder)
                }
            }
        }

        private fun getCreationTimeStamp(creationTimestamp: Long): String {
            val date = Date(creationTimestamp)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return "created at:" + dateFormat.format(date)
        }
    }

    interface RemindersListCallback {
        fun onDeleteClicked(reminder: Reminder)
    }
}