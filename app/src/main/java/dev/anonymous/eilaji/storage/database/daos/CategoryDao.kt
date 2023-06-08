package dev.anonymous.eilaji.storage.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import dev.anonymous.eilaji.storage.database.tables.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category")
    fun getAll(): LiveData<List<Category>>

    @Query("SELECT * FROM Category WHERE cid IN (:categoryIds)")
    fun loadAllByIds(categoryIds: IntArray): List<Category>

    @Query(
        "SELECT * FROM Category WHERE name LIKE :name AND " +
                "rank LIKE :rank LIMIT 1"
    )
    fun findByNameAndRank(name: String, rank: String): Category

    @Insert
    fun insertAll(vararg category: Category)

    @Delete
    fun delete(category: Category)
}