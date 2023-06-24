package dev.anonymous.eilaji.storage.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anonymous.eilaji.storage.database.tables.FavoriteMedicine
import dev.anonymous.eilaji.storage.database.tables.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMedicineDao {
    @Query("SELECT 'medicine.*' FROM 'medicines' INNER JOIN favorite_medicine ON 'medicine.id' = favorite_medicine.medicine_id WHERE favorite_medicine.patient_id = :patientId")
    fun getFavoriteMedicinesForPatient(patientId: Int): Flow<List<Medicine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteMedicine(favoriteMedicine: FavoriteMedicine)

    @Query("DELETE FROM favorite_medicine WHERE patient_id = :patientId AND medicine_id = :medicineId")
    suspend fun deleteFavoriteMedicine(patientId: Int, medicineId: Int)
}
