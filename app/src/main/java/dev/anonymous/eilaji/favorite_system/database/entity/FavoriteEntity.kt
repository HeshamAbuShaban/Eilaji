package dev.anonymous.eilaji.favorite_system.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "medicineId") val medicineId: String?,
    @ColumnInfo(name = "pharmacyId") val pharmacyId: String?,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "medicineTitleEn") val medicineTitleEn: String?,
    @ColumnInfo(name = "medicineTitleAr") val medicineTitleAr: String?,
    @ColumnInfo(name = "pharmacyName") val pharmacyName: String?,
    @ColumnInfo(name = "createdAt") val createdAt: String,
    @ColumnInfo(name = "backendId") val backendId: String?,
    @ColumnInfo(name = "syncStatus") val syncStatus: String
)
