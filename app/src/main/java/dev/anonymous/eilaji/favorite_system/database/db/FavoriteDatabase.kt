package dev.anonymous.eilaji.favorite_system.database.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.anonymous.eilaji.favorite_system.database.dao.FavoriteDao
import dev.anonymous.eilaji.favorite_system.database.entity.FavoriteEntity

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class FavoriteDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var instance: FavoriteDatabase? = null

        fun getDatabase(context: Context): FavoriteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, FavoriteDatabase::class.java, "favorites.db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build().also { instance = it }
            }
        }
    }
}
