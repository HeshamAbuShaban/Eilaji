package dev.anonymous.eilaji.reminder_system.util

import dev.anonymous.eilaji.network.CreateReminderRequest
import dev.anonymous.eilaji.network.MedicationReminderDto
import dev.anonymous.eilaji.network.UpdateReminderRequest
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import java.time.Instant

object ReminderMapper {
    fun toDto(r: Reminder): MedicationReminderDto = MedicationReminderDto(
        id = r.backendId ?: r.id,
        medicineName = r.medicineName ?: r.text ?: "Medicine",
        dosage = r.dosage,
        frequency = r.frequency ?: "DAILY",
        scheduleTime = r.scheduleTime ?: "08:00:00",
        customDays = r.getCustomDaysList(),
        notes = r.notes,
        isActive = r.isActive,
        startDate = r.startDate?.let { Instant.ofEpochMilli(it).toString() },
        endDate = r.endDate?.let { Instant.ofEpochMilli(it).toString() }
    )
    fun toCreate(r: Reminder): CreateReminderRequest = CreateReminderRequest(
        medicineName = r.medicineName ?: r.text ?: "Medicine",
        dosage = r.dosage,
        frequency = r.frequency ?: "DAILY",
        scheduleTime = r.scheduleTime ?: "08:00:00",
        customDays = r.getCustomDaysList(),
        notes = r.notes,
        isActive = r.isActive,
        startDate = r.startDate?.let { Instant.ofEpochMilli(it).toString() },
        endDate = r.endDate?.let { Instant.ofEpochMilli(it).toString() }
    )
    fun toUpdate(r: Reminder): UpdateReminderRequest = UpdateReminderRequest(
        medicineName = r.medicineName ?: r.text,
        dosage = r.dosage,
        frequency = r.frequency,
        scheduleTime = r.scheduleTime,
        customDays = r.getCustomDaysList(),
        notes = r.notes,
        isActive = r.isActive,
        endDate = r.endDate?.let { Instant.ofEpochMilli(it).toString() }
    )
    fun fromDto(d: MedicationReminderDto): Reminder {
        val rem = Reminder(d.id, d.medicineName, 0L, 1)
        rem.medicineName = d.medicineName
        rem.dosage = d.dosage
        rem.frequency = d.frequency
        rem.scheduleTime = ReminderTimeUtils.parseScheduleTime(d.scheduleTime)?.let { ReminderTimeUtils.formatScheduleTime(it) } ?: "08:00:00"
        rem.setCustomDaysList(d.customDays)
        rem.notes = d.notes
        rem.isActive = d.isActive
        rem.backendId = d.id
        rem.syncStatus = "SYNCED"
        try { d.startDate?.let { rem.startDate = Instant.parse(it).toEpochMilli() } } catch (_: Exception) {}
        try { d.endDate?.let { rem.endDate = Instant.parse(it).toEpochMilli() } } catch (_: Exception) {}
        return rem
    }
}
