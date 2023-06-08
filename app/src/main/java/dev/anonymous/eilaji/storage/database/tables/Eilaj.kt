package dev.anonymous.eilaji.storage.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Eilaj(
    @PrimaryKey val eid: Int,
    @ColumnInfo(name = "name") val eilajName: String?,
    @ColumnInfo(name = "category") val eilajCategory: String?
)
