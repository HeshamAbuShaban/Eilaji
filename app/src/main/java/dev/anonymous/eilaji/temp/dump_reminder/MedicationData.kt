package dev.anonymous.eilaji.temp.dump_reminder

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationName: String,
    val dosage: String,
    val reminderDate: Date
)

@Dao
interface MedicationReminderDao {
    @Insert
    suspend fun addReminder(reminder: MedicationReminderEntity)

    @Update
    suspend fun updateReminder(reminder: MedicationReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminderEntity)

    @Query("SELECT * FROM medication_reminders")
    fun getAllReminders(): Flow<List<MedicationReminderEntity>>
}
