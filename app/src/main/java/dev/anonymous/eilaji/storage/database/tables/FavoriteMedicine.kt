package dev.anonymous.eilaji.storage.database.tables

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "favorite_medicine",
    primaryKeys = ["patient_id", "medicine_id"],
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoriteMedicine(
    val patient_id: Int,
    val medicine_id: Int
)
