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
import dev.anonymous.eilaji.reminder_system.repository.ReminderSyncRepository
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.other.dialogs.DeleteItemDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.DeleteItemDialogFragment.DeleteItemDialogListener

class RemindersListFragment : Fragment(), RemindersAdapter.RemindersListCallback, DeleteItemDialogListener {
    private lateinit var binding: FragmentRemindersListBinding
    private lateinit var vm: RemindersListViewModel
    private lateinit var scheduler: ReminderScheduler
    private lateinit var dbVm: ReminderDatabaseViewModel
    private lateinit var adapter: RemindersAdapter
    private lateinit var syncRepo: ReminderSyncRepository
    private lateinit var pendingDelete: Reminder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRemindersListBinding.inflate(layoutInflater)
        vm = ViewModelProvider(this)[RemindersListViewModel::class.java]
        dbVm = ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        vm.setupReminderDataViewModel(dbVm)
        scheduler = ReminderScheduler(requireContext().applicationContext)
        vm.setReminderScheduler(scheduler)
        syncRepo = ReminderSyncRepository(requireContext().applicationContext)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabAddAReminder.setOnClickListener { findNavController().navigate(R.id.action_navigation_reminders_list_to_navigation_add_reminder) }
        binding.recViewRemindersList.setHasFixedSize(false)
        vm.getAllReminders().observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) showEmpty() else { hideEmpty(); adapter = RemindersAdapter(list as ArrayList<Reminder>); adapter.registerRemindersListCallback(this); binding.recViewRemindersList.adapter = adapter }
        }
        syncRepo.syncFetch { remote ->
            if (remote != null && remote.isNotEmpty()) {
                remote.forEach { r -> try { dbVm.insertReminder(r); scheduler.setReminderObject(r); if (r.isActive()) scheduler.scheduleExact(r.id, r.medicineName ?: r.text, r.notificationId, r.frequency ?: "DAILY", r.customDays ?: "[]", r.scheduleTime ?: "08:00:00") } catch (_: Exception) {} }
            }
        }
    }
    private fun showEmpty() {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.emptyListText.text = SpannableStringBuilder().apply { bold { append(getString(R.string.empty_reminders)) }; append("\ncreate a reminder and it will show up here.") }
    }
    private fun hideEmpty() { binding.emptyStateContainer.visibility = View.GONE }
    override fun onDeleteClicked(r: Reminder) { pendingDelete = r; DeleteItemDialogFragment().show(childFragmentManager, "DeleteItemTriggered") }
    override fun onToggleActive(r: Reminder, active: Boolean) {
        dbVm.let {
            val upd = r; upd.setActive(active)
            it.let { try { it.javaClass.getMethod("updateReminder", Reminder::class.java).invoke(it, upd) } catch (_: Exception) { } }
            if (active) { scheduler.setReminderObject(r); scheduler.scheduleExact(r.id, r.medicineName ?: r.text, r.notificationId, r.frequency ?: "DAILY", r.customDays ?: "[]", r.scheduleTime ?: "08:00:00") } else scheduler.cancelReminderById(r)
            syncRepo.syncUpdate(r)
        }
    }
    override fun onDialogDeleteClicked() {
        vm.deleteReminder(pendingDelete)
        syncRepo.syncDelete(pendingDelete)
        try {
            val pos = adapter.remindersList.indexOf(pendingDelete)
            if (pos != -1) { adapter.remindersList.removeAt(pos); adapter.notifyItemRemoved(pos); adapter.notifyItemRangeChanged(pos, adapter.remindersList.size) }
        } catch (_: Exception) {}
    }
}
