package dev.anonymous.eilaji.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.anonymous.eilaji.storage.database.daos.CategoryDao
import dev.anonymous.eilaji.storage.database.daos.FavoriteMedicineDao
import dev.anonymous.eilaji.storage.database.daos.MedicineDao
import dev.anonymous.eilaji.storage.database.daos.PatientDao
import dev.anonymous.eilaji.storage.database.tables.Category
import dev.anonymous.eilaji.storage.database.tables.FavoriteMedicine
import dev.anonymous.eilaji.storage.database.tables.Medicine
import dev.anonymous.eilaji.storage.database.tables.Patient
import android.content.Context
import androidx.room.Room

@Database(entities = [Category::class, Medicine::class, Patient::class, FavoriteMedicine::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun medicineDao(): MedicineDao
    abstract fun patientDao(): PatientDao
    abstract fun favoriteMedicineDao(): FavoriteMedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
