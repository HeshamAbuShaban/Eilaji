package dev.anonymous.eilaji.ui.other.reminder.list

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.RemindersAdapter
import dev.anonymous.eilaji.databinding.FragmentRemindersListBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler

class RemindersListFragment : Fragment(), RemindersAdapter.RemindersListCallback {
    private lateinit var binding: FragmentRemindersListBinding
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var reminderDatabaseViewModel: ReminderDatabaseViewModel
    private lateinit var remindersAdapter: RemindersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRemindersListBinding.inflate(layoutInflater)
        setupVM()
        return binding.root
    }

    private fun setupVM() {
        // view ViewModel
        remindersListViewModel = ViewModelProvider(this)[RemindersListViewModel::class.java]
        // Database
        reminderDatabaseViewModel =
            ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        remindersListViewModel.setupReminderDataViewModel(reminderDatabaseViewModel)

        // rs Reminder Scheduler
        reminderScheduler = ReminderScheduler(requireContext().applicationContext)
        // set it to the v_view model
        remindersListViewModel.setReminderScheduler(reminderScheduler)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecView()
        setupListeners()
    }


    private fun setupListeners() {
        with(binding.fabAddAReminder) {
            setAnimateShowBeforeLayout(true)
            setOnClickListener {
                findNavController().navigate(R.id.action_navigation_reminders_list_to_navigation_add_reminder)
            }
        }
    }

    private fun setupRecView() {
        with(binding.recViewRemindersList) {
            setHasFixedSize(false)
            remindersListViewModel.getAllReminders().observe(requireActivity()) { reminders ->
                if (reminders.isEmpty()) {
                    // Show the empty image
                    showEmptyStates()
                } else {
                    // Hide the empty image
                    hideEmptyStates()
                    remindersAdapter = RemindersAdapter(reminders as ArrayList<Reminder>)
                    remindersAdapter.registerRemindersListCallback(this@RemindersListFragment)
                    adapter = remindersAdapter
                }
            }
        }
    }

    private fun showEmptyStates() {
        // Show the empty image view or set its visibility to visible
        with(binding) {
            root.setBackgroundResource(R.color.c1_reminder_item_)
            emptyListImage.visibility = View.VISIBLE
            with(emptyListText) {
                visibility = View.VISIBLE
                val boldText = getString(R.string.empty_reminders)
                val normalText = "create a reminder and it will show up here."

                val spannableString = SpannableStringBuilder()
                spannableString.bold { append(boldText) }
                spannableString.append("\n")
                spannableString.append(normalText)

                text = spannableString
//                text = getString(R.string.empty_reminders, "create a reminder and it will show up here.")
            }
        }

    }

    private fun hideEmptyStates() {
        // Hide the empty image view or set its visibility to gone
        with(binding) {
            root.setBackgroundResource(R.color.c1_reminder_item)
            emptyListImage.visibility = View.GONE
            emptyListText.visibility = View.GONE
        }
    }

    override fun onDeleteClicked(reminder: Reminder) {
        remindersListViewModel.deleteReminder(reminder)
        val position = remindersAdapter.remindersList.indexOf(reminder)
        if (position != -1) {
            with(remindersAdapter) {
                remindersList.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(
                    position,
                    remindersList.size
                ) // Notify the adapter that item positions have changed
            }
            binding.recViewRemindersList.smoothScrollToPosition(position + 1)
        }
    }

}