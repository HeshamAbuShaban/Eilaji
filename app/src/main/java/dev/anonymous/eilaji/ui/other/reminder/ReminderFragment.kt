package dev.anonymous.eilaji.ui.other.reminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.repository.ReminderSyncRepository
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment.RequestPermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class ReminderFragment : Fragment(), RequestPermissionsListener {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var reminderViewModel: ReminderViewModel
    private lateinit var binding: FragmentReminderBinding
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var reminderDatabaseViewModel: ReminderDatabaseViewModel
    private lateinit var syncRepo: ReminderSyncRepository

    enum class Mode { ONCE, DAILY, DAYS, INTERVAL }
    private var mode: Mode = Mode.DAILY
    private var customDays: List<String> = emptyList()
    private var intervalVal: Long = 2
    private var intervalUnit: TimeUnit = TimeUnit.DAYS
    private var intervalIsWeeks: Boolean = false

    private val suggestHandler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null
    private var suggestCall: Call<ApiResponse<PaginatedResult<MedicineDto>>>? = null
    private var catalogDosage: Map<String, String> = emptyMap()

    private val dayKeys = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReminderBinding.inflate(layoutInflater)
        setupComponents()
        reminderViewModel.determinedTheBackGround(binding)
        return binding.root
    }

    private fun setupComponents() {
        reminderViewModel = ViewModelProvider(this)[ReminderViewModel::class.java]
        reminderScheduler = ReminderScheduler(requireContext().applicationContext)
        reminderViewModel.setReminderScheduler(reminderScheduler)
        reminderDatabaseViewModel = ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        reminderViewModel.setDatabaseViewModel(reminderDatabaseViewModel)
        syncRepo = ReminderSyncRepository(requireContext().applicationContext)
    }

    override fun onResume() { super.onResume(); requireActivity().onBackPressedDispatcher.addCallback(this) { findNavController().popBackStack() } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.all { it.value }) save()
            else Toast.makeText(requireContext(), "Permission denied. Cannot create reminder.", Toast.LENGTH_SHORT).show()
        }
        if (isBatteryOptimizationEnabled()) showBatteryOptimizationDialog()
        setupTimeSection()
        setupRepeatSection()
        setupAutocomplete()
        try { dev.anonymous.eilaji.utils.SpringFx.pressable(binding.buSaveReminder) } catch (_: Exception) {}
        binding.buSaveReminder.setOnClickListener { onSavePressed() }
        renderTime()
        renderInterval()
        renderSummary()
    }

    // ---------- time ----------

    private fun setupTimeSection() {
        renderTime()
        binding.tvTimeBig.setOnClickListener { openTimePicker() }
        binding.chipMorning.setOnClickListener { setTime(8, 0) }
        binding.chipNoon.setOnClickListener { setTime(13, 0) }
        binding.chipEvening.setOnClickListener { setTime(18, 0) }
        binding.chipNight.setOnClickListener { setTime(22, 0) }
    }

    private fun setTime(h: Int, m: Int) {
        reminderViewModel.hour = h
        reminderViewModel.minute = m
        renderTime()
    }

    private fun renderTime() {
        try {
            binding.tvTimeBig.text = String.format("%02d:%02d", reminderViewModel.hour, reminderViewModel.minute)
            binding.tvNextDoseLive.text = reminderViewModel.nextDoseText()
            renderSummary()
        } catch (_: Exception) {}
    }

    private fun openTimePicker() {
        try {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(reminderViewModel.hour)
                .setMinute(reminderViewModel.minute)
                .setTitleText("Dose time")
                .build()
            picker.addOnPositiveButtonClickListener { setTime(picker.hour, picker.minute) }
            picker.show(childFragmentManager, "DoseTimePicker")
        } catch (_: Exception) {}
    }

    // ---------- repeat ----------

    private fun setupRepeatSection() {
        binding.segFrequency.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            mode = when (checkedId) {
                R.id.segOnce -> Mode.ONCE
                R.id.segDaily -> Mode.DAILY
                R.id.segDays -> Mode.DAYS
                else -> Mode.INTERVAL
            }
            binding.chipCustomDays.visibility = if (mode == Mode.DAYS) View.VISIBLE else View.GONE
            binding.intervalRow.visibility = if (mode == Mode.INTERVAL) View.VISIBLE else View.GONE
            binding.intervalUnitRow.visibility = if (mode == Mode.INTERVAL) View.VISIBLE else View.GONE
            renderSummary()
        }
        val group = binding.chipCustomDays
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
            chip.setOnCheckedChangeListener { _, _ -> collectChips(); renderSummary() }
        }
        binding.buIntervalMinus.setOnClickListener { intervalVal = (intervalVal - 1).coerceAtLeast(1); renderInterval(); renderSummary() }
        binding.buIntervalPlus.setOnClickListener { intervalVal = (intervalVal + 1).coerceAtMost(30); renderInterval(); renderSummary() }
        binding.chipUnitHours.setOnClickListener { setUnit(TimeUnit.HOURS) }
        binding.chipUnitDays.setOnClickListener { setUnit(TimeUnit.DAYS) }
        binding.chipUnitWeeks.setOnClickListener { setUnitWeeks() }
        renderInterval()
    }

    private fun setUnit(u: TimeUnit) {
        intervalUnit = u
        intervalIsWeeks = false
        binding.chipUnitHours.isChecked = u == TimeUnit.HOURS
        binding.chipUnitDays.isChecked = u == TimeUnit.DAYS
        binding.chipUnitWeeks.isChecked = false
        renderInterval(); renderSummary()
    }

    private fun setUnitWeeks() {
        intervalUnit = TimeUnit.DAYS
        intervalIsWeeks = true
        binding.chipUnitHours.isChecked = false
        binding.chipUnitDays.isChecked = false
        binding.chipUnitWeeks.isChecked = true
        renderInterval(); renderSummary()
    }

    private fun renderInterval() {
        val unitName = when {
            intervalIsWeeks -> if (intervalVal == 1L) "week" else "weeks"
            intervalUnit == TimeUnit.HOURS -> if (intervalVal == 1L) "hour" else "hours"
            else -> if (intervalVal == 1L) "day" else "days"
        }
        binding.tvIntervalValue.text = "Every $intervalVal $unitName"
    }

    private fun collectChips() {
        try {
            val picked = mutableListOf<String>()
            val group = binding.chipCustomDays
            for (i in 0 until group.childCount) {
                val chip = group.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
                if (chip.isChecked) picked.add(dayKeys.getOrElse(i) { "" })
            }
            customDays = picked.filter { it.isNotBlank() }
        } catch (_: Exception) {}
    }

    // ---------- autocomplete: catalog + your past names, your text always wins ----------

    private fun setupAutocomplete() {
        binding.actMedicineName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                suggestRunnable?.let { suggestHandler.removeCallbacks(it) }
                val q = s?.toString()?.trim().orEmpty()
                renderSummary()
                if (q.length < 2) return
                suggestRunnable = Runnable { fetchSuggestions(q) }
                suggestHandler.postDelayed(suggestRunnable!!, 300)
            }
        })
        binding.actMedicineName.setOnItemClickListener { parent, _, pos, _ ->
            val picked = parent.getItemAtPosition(pos).toString()
            binding.actMedicineName.setText(picked)
            binding.actMedicineName.setSelection(picked.length)
            catalogDosage[picked]?.let {
                if (binding.reminderDosageEditText.text.toString().isBlank()) binding.reminderDosageEditText.setText(it)
            }
            renderSummary()
        }
    }

    private fun fetchSuggestions(q: String) {
        try { suggestCall?.cancel() } catch (_: Exception) {}
        val past = pastNames()
        val call = try { NetworkModule.provideApiService(requireContext()).searchMedicines(q, 0, 8) }
        catch (_: Exception) { showSuggestions(past); return }
        suggestCall = call
        call.enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (call.isCanceled) return
                val meds = response.body()?.data?.items ?: emptyList()
                val doseMap = meds.mapNotNull { m ->
                    val t = m.titleEn.ifBlank { m.titleAr }
                    if (t.isBlank()) null else { m.dosage?.let { d -> t to d }; t }
                }
                catalogDosage = meds.mapNotNull { m ->
                    val t = m.titleEn.ifBlank { m.titleAr }
                    if (t.isBlank() || m.dosage.isNullOrBlank()) null else t to m.dosage!!
                }.toMap()
                val names = (past + doseMap).distinct().take(8)
                showSuggestions(names)
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                if (!call.isCanceled) showSuggestions(past)
            }
        })
    }

    private fun pastNames(): List<String> {
        return try {
            val db = dev.anonymous.eilaji.reminder_system.database.db.ReminderDatabase.getDatabase(requireContext())
            db.reminderDao().getAllSync().mapNotNull { it.getMedicineName()?.takeIf { n -> n.isNotBlank() } }.distinct()
        } catch (_: Exception) { emptyList() }
    }

    private fun showSuggestions(names: List<String>) {
        try {
            if (!isAdded) return
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
            binding.actMedicineName.setAdapter(adapter)
            if (names.isNotEmpty() && binding.actMedicineName.hasFocus()) binding.actMedicineName.showDropDown()
        } catch (_: Exception) {}
    }

    // ---------- summary + save ----------

    private fun freqLabel(): String {
        return when (mode) {
            Mode.ONCE -> "Once"
            Mode.DAILY -> "Daily"
            Mode.DAYS -> if (customDays.isEmpty()) "Pick days" else customDays.joinToString(", ") { it.take(3) }
            Mode.INTERVAL -> {
                val u = when {
                    intervalIsWeeks -> if (intervalVal == 1L) "week" else "weeks"
                    intervalUnit == TimeUnit.HOURS -> if (intervalVal == 1L) "hour" else "hours"
                    else -> if (intervalVal == 1L) "day" else "days"
                }
                "Every $intervalVal $u"
            }
        }
    }

    private fun renderSummary() {
        try {
            val name = binding.actMedicineName.text.toString().trim().ifBlank { "—" }
            val time = String.format("%02d:%02d", reminderViewModel.hour, reminderViewModel.minute)
            binding.tvSummary.text = "$name · ${freqLabel()} · $time"
        } catch (_: Exception) {}
    }

    private fun ensureFullScreenPermission(): Boolean {
        // Android 14+: full-screen alarm needs explicit user grant, else only the notification shows.
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (!nm.canUseFullScreenIntent()) {
                    Snackbar.make(binding.root, "Allow full-screen alarms for the loud alert", Snackbar.LENGTH_LONG)
                        .setAction("Settings") {
                            try {
                                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                                })
                            } catch (_: Exception) {}
                        }.show()
                    return false
                }
            }
            true
        } catch (_: Exception) { true }
    }

    private fun onSavePressed() {
        if (!ensureFullScreenPermission()) return
        val name = binding.actMedicineName.text.toString().trim()
        if (name.isEmpty()) {
            try {
                (binding.actMedicineName.parent.parent as? com.google.android.material.textfield.TextInputLayout)?.error = "Enter a medicine name"
            } catch (_: Exception) {}
            binding.actMedicineName.requestFocus()
            return
        }
        if (mode == Mode.DAYS && customDays.isEmpty()) {
            Snackbar.make(binding.root, "Pick at least one day", Snackbar.LENGTH_SHORT).show()
            return
        }
        if (!arePermissionsGranted()) {
            RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_we_are_sorry)).show(childFragmentManager, "WeAreSorry")
            return
        }
        save()
    }

    private fun save() {
        val txt = binding.actMedicineName.text.toString().trim()
        val dosage = try { binding.reminderDosageEditText.text.toString().trim().ifBlank { null } } catch (_: Exception) { null }
        val isActive = try { binding.switchIsActive.isChecked } catch (_: Exception) { true }
        val freq: String
        val interval: Long
        val unit: TimeUnit
        val days: List<String>
        when (mode) {
            Mode.ONCE -> { freq = "ONCE"; interval = 1; unit = TimeUnit.DAYS; days = emptyList() }
            Mode.DAILY -> { freq = "DAILY"; interval = 1; unit = TimeUnit.DAYS; days = emptyList() }
            Mode.DAYS -> { freq = "CUSTOM"; interval = 1; unit = TimeUnit.DAYS; days = customDays }
            Mode.INTERVAL -> {
                // hours / days / weeks only (minutes dropped by design)
                if (intervalIsWeeks) { freq = "CUSTOM"; interval = intervalVal * 7; unit = TimeUnit.DAYS; days = emptyList() }
                else if (intervalUnit == TimeUnit.HOURS) { freq = "CUSTOM"; interval = intervalVal; unit = TimeUnit.HOURS; days = emptyList() }
                else {
                    if (intervalVal == 1L) { freq = "DAILY"; interval = 1; unit = TimeUnit.DAYS; days = emptyList() }
                    else if (intervalVal == 7L) { freq = "WEEKLY"; interval = 7; unit = TimeUnit.DAYS; days = emptyList() }
                    else { freq = "CUSTOM"; interval = intervalVal; unit = TimeUnit.DAYS; days = emptyList() }
                }
            }
        }
        val scheduleTime = reminderViewModel.buildScheduleTime()
        val id = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
        val reminder = Reminder(id, if (dosage != null) "$txt — $dosage" else txt, 60, if (freq == "DAILY") 1 else 2)
        reminder.medicineName = txt
        reminder.dosage = dosage
        reminder.frequency = freq
        reminder.scheduleTime = scheduleTime
        reminder.setCustomDaysList(days)
        reminder.setActive(isActive)
        reminder.startDate = System.currentTimeMillis()
        reminder.syncStatus = "PENDING"
        reminderViewModel.reminderScheduler.value?.setReminderObject(reminder)
        if (isActive) {
            ensureExactAlarmPermission()
            val sub15 = try { unit.toMinutes(interval) < 15 } catch (_: Exception) { false }
            if (freq == "ONCE" || sub15) {
                try { reminderViewModel.reminderScheduler.value?.scheduleExact(reminder.id, txt, reminder.notificationId, freq, days.toString(), scheduleTime) } catch (_: Exception) {}
                reminderViewModel.reminderScheduler.value?.scheduleReminderOneTimeWorkRequest()
            } else {
                reminderViewModel.reminderScheduler.value?.scheduleReminderPeriodicWorkRequest(interval, unit)
                try { reminderViewModel.reminderScheduler.value?.scheduleExact(reminder.id, txt, reminder.notificationId, freq, days.toString(), scheduleTime) } catch (_: Exception) {}
            }
        }
        reminderViewModel.storeReminderIntoDatabase(reminder)
        syncRepo.syncCreate(reminder) { ok -> if (ok) reminder.syncStatus = "SYNCED" }
        goToList(reminder.id)
    }

    private fun ensureExactAlarmPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (!am.canScheduleExactAlarms()) {
                    try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    private fun goToList(highlightId: String) {
        try {
            val b = Bundle().apply { putString("highlightId", highlightId) }
            val opts = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(dev.anonymous.eilaji.R.id.navigation_add_reminder, true)
                .build()
            findNavController().navigate(dev.anonymous.eilaji.R.id.navigation_reminders_list, b, opts)
        } catch (_: Exception) {
            try { findNavController().popBackStack() } catch (_: Exception) {}
        }
    }

    private fun arePermissionsGranted() = REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
    private fun isBatteryOptimizationEnabled(): Boolean {
        val pm = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(requireContext().packageName)
    }
    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Battery Optimization").setMessage(getString(R.string.battery_optimization))
            .setPositiveButton("Go to Settings") { _, _ -> startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .setNegativeButton("Later", null).create().show()
    }
    private fun requestPermissions() { requestPermissionLauncher.launch(REQUIRED_PERMISSIONS) }
    override fun onAllowClicked() { requestPermissions() }
    override fun onDenyClicked() { Toast.makeText(requireContext(), getString(R.string.permissions_message_sorry_you_can_not), Toast.LENGTH_SHORT).show() }
    override fun onDestroyView() {
        try { suggestCall?.cancel() } catch (_: Exception) {}
        try { suggestRunnable?.let { suggestHandler.removeCallbacks(it) } } catch (_: Exception) {}
        super.onDestroyView()
    }
    companion object {
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.VIBRATE) else arrayOf(Manifest.permission.VIBRATE)
    }
}
