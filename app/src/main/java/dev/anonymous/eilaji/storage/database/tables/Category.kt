package dev.anonymous.eilaji.storage.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Category(
    @PrimaryKey val cid: Int,
    @ColumnInfo(name = "name") val categoryName: String?,
    @ColumnInfo(name = "rank") val rank: String?
)