package dev.anonymous.eilaji.storage.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anonymous.eilaji.storage.database.tables.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patient")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patient WHERE id = :patientId")
    fun getPatientById(patientId: Int): Flow<Patient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Query("DELETE FROM patient WHERE id = :patientId")
    suspend fun deletePatient(patientId: Int)
}
