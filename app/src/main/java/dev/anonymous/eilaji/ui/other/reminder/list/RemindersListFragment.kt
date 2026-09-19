package dev.anonymous.eilaji.ui.other.reminder.list

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
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
        binding = FragmentRemindersListBinding.inflate(inflater, container, false)
        vm = ViewModelProvider(this)[RemindersListViewModel::class.java]
        dbVm = ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        vm.setupReminderDataViewModel(dbVm)
        scheduler = ReminderScheduler(requireContext().applicationContext)
        vm.setReminderScheduler(scheduler)
        syncRepo = ReminderSyncRepository(requireContext().applicationContext)
        return binding.root
    }
    private var highlightId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        highlightId = arguments?.getString("highlightId")
        binding.fabAddAReminder.setOnClickListener { findNavController().navigate(R.id.action_navigation_reminders_list_to_navigation_add_reminder) }
        binding.recViewRemindersList.setHasFixedSize(false)
        vm.getAllReminders().observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                showEmpty()
            } else {
                hideEmpty()
                renderSummary(list)
                adapter = RemindersAdapter(list as ArrayList<Reminder>)
                adapter.registerRemindersListCallback(this)
                binding.recViewRemindersList.adapter = adapter
                attachSwipe()
                highlightId?.let { flashNew(it, list) }
            }
        }
        syncRepo.syncFetch { remote ->
            if (remote != null && remote.isNotEmpty()) {
                remote.forEach { r -> try { dbVm.insertReminder(r); scheduler.setReminderObject(r); if (r.isActive()) scheduler.scheduleExact(r.id, r.medicineName ?: r.text, r.notificationId, r.frequency ?: "DAILY", r.customDays ?: "[]", r.scheduleTime ?: "08:00:00") } catch (_: Exception) {} }
            }
        }
    }

    private fun attachSwipe() {
        val bg = ColorDrawable(Color.parseColor("#FF3B30"))
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_reminder)
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.adapterPosition
                if (pos != -1 && pos < adapter.remindersList.size) {
                    pendingDelete = adapter.remindersList[pos]
                    DeleteItemDialogFragment().show(childFragmentManager, "DeleteItemTriggered")
                    adapter.notifyItemChanged(pos)
                }
            }
            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                val itemView = vh.itemView
                if (dX < 0) { bg.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom); bg.draw(c) }
                else if (dX > 0) { bg.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom); bg.draw(c) }
                deleteIcon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                    val iconBottom = iconTop + it.intrinsicHeight
                    if (dX < 0) { val l = itemView.right - iconMargin - it.intrinsicWidth; it.setBounds(l, iconTop, itemView.right - iconMargin, iconBottom) }
                    else { val l = itemView.left + iconMargin; it.setBounds(l, iconTop, l + it.intrinsicWidth, iconBottom) }
                    it.draw(c)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recViewRemindersList)
    }

    private fun renderSummary(list: List<Reminder>) {
        try {
            val active = list.filter { it.isActive() }
            if (active.isEmpty()) {
                binding.cardTodaySummary.visibility = View.GONE
                return
            }
            binding.cardTodaySummary.visibility = View.VISIBLE
            binding.tvTodayCount.text = if (active.size == 1) "1 dose today" else "${active.size} doses today"
            val next = active.minByOrNull { nextIn(it) }
            binding.tvNextDoseSummary.text = next?.let {
                "Next: ${it.medicineName ?: it.text} at ${it.scheduleTime?.substring(0, 5)}"
            } ?: ""
        } catch (_: Exception) {}
    }

    private fun nextIn(r: Reminder): Long {
        return try {
            val st = r.scheduleTime ?: return Long.MAX_VALUE
            val lt = dev.anonymous.eilaji.reminder_system.util.ReminderTimeUtils.parseScheduleTime(st) ?: return Long.MAX_VALUE
            val now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
            var cand = now.withHour(lt.hour).withMinute(lt.minute).withSecond(0).withNano(0)
            if (!cand.isAfter(now)) cand = cand.plusDays(1)
            cand.toInstant().toEpochMilli()
        } catch (_: Exception) { Long.MAX_VALUE }
    }

    private fun flashNew(id: String, list: List<Reminder>) {
        try {
            highlightId = null
            val pos = list.indexOfFirst { it.id == id }
            if (pos == -1) return
            binding.recViewRemindersList.scrollToPosition(pos)
            binding.recViewRemindersList.postDelayed({
                try {
                    val holder = binding.recViewRemindersList.findViewHolderForAdapterPosition(pos)
                    holder?.itemView?.let { v ->
                        v.alpha = 0.3f
                        v.animate().alpha(1f).setDuration(450).start()
                        com.google.android.material.snackbar.Snackbar.make(binding.root, "Reminder saved", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {}
            }, 350)
        } catch (_: Exception) {}
    }

    private fun showEmpty() {
        binding.emptyStateContainer.visibility = View.VISIBLE
        try { binding.cardTodaySummary.visibility = View.GONE } catch (_: Exception) {}
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
            if (pos != -1) { adapter.remindersList.removeAt(pos); adapter.notifyItemRemoved(pos); adapter.notifyItemRangeChanged(pos, adapter.remindersList.size); if (adapter.remindersList.isEmpty()) showEmpty() }
        } catch (_: Exception) {}
    }
}
