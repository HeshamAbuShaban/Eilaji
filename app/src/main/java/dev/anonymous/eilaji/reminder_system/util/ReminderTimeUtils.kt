package dev.anonymous.eilaji.reminder_system.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ReminderTimeUtils {
    private val tfs = listOf(
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm")
    )
    fun parseScheduleTime(input: String?): LocalTime? {
        if (input.isNullOrBlank()) return null
        val s = input.trim()
        for (f in tfs) try { return LocalTime.parse(s, f) } catch (_: DateTimeParseException) {}
        return try { LocalTime.parse(s) } catch (_: Exception) { null }
    }
    fun formatScheduleTime(t: LocalTime): String = t.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    fun formatDisplay(t: LocalTime): String = t.format(DateTimeFormatter.ofPattern("hh:mm a"))
    fun nextTriggerMillis(scheduleTime: String, freq: String, customDays: List<String>): Long {
        val lt = parseScheduleTime(scheduleTime) ?: LocalTime.of(8, 0)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var cand = now.withHour(lt.hour).withMinute(lt.minute).withSecond(lt.second).withNano(0)
        if (!cand.isAfter(now)) cand = cand.plusDays(1)
        when (freq.uppercase()) {
            "DAILY" -> return cand.toInstant().toEpochMilli()
            "WEEKLY" -> {
                var d = cand
                repeat(7) {
                    if (d.isAfter(now)) return d.toInstant().toEpochMilli()
                    d = d.plusDays(1)
                }
                return cand.plusWeeks(1).toInstant().toEpochMilli()
            }
            "CUSTOM" -> {
                if (customDays.isEmpty()) return cand.toInstant().toEpochMilli()
                val want = customDays.mapNotNull { runCatching { DayOfWeek.valueOf(it.uppercase()) }.getOrNull() }.toSet()
                if (want.isEmpty()) return cand.toInstant().toEpochMilli()
                var d = cand
                repeat(14) {
                    if (d.dayOfWeek in want && d.isAfter(now)) return d.toInstant().toEpochMilli()
                    d = d.plusDays(1).withHour(lt.hour).withMinute(lt.minute).withSecond(0).withNano(0)
                }
                return d.toInstant().toEpochMilli()
            }
            else -> return cand.toInstant().toEpochMilli()
        }
    }
    fun isActiveToday(freq: String, customDays: List<String>): Boolean {
        if (freq.uppercase() != "CUSTOM") return true
        if (customDays.isEmpty()) return true
        val today = LocalDate.now().dayOfWeek.name
        return customDays.any { it.equals(today, true) || it.equals(today.take(3), true) }
    }
}
