package dev.anonymous.eilaji.temp.dump_reminder

import java.util.Date

data class MedicationReminder( val id: Long,
                               val medicationName: String,
                               val dosage: String,
                               val reminderDate: Date)
