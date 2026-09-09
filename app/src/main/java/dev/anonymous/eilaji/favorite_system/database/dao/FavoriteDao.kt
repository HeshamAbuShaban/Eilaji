package dev.anonymous.eilaji.favorite_system.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.anonymous.eilaji.favorite_system.database.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllLive(): LiveData<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllSync(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE syncStatus = 'PENDING'")
    fun getPendingSync(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE medicineId = :medId AND pharmacyId IS :pharmId LIMIT 1")
    fun findByMedicineAndPharmacy(medId: String?, pharmId: String?): FavoriteEntity?

    @Query("SELECT * FROM favorites WHERE medicineId = :medId LIMIT 1")
    fun findByMedicineId(medId: String): FavoriteEntity?

    @Query("SELECT * FROM favorites WHERE pharmacyId = :pharmId LIMIT 1")
    fun findByPharmacyId(pharmId: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM favorites")
    fun clearAll()

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    fun getById(id: String): FavoriteEntity?
}
