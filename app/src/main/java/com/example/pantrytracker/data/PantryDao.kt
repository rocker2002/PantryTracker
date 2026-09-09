package com.example.pantrytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY expiryDate ASC")
    fun getAllItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE isConsumed = 0 ORDER BY expiryDate ASC")
    fun getActiveItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    fun getItemById(id: Long): Flow<PantryItem?>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getItemByIdOnce(id: Long): PantryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PantryItem): Long

    @Update
    suspend fun update(item: PantryItem)

    @Delete
    suspend fun delete(item: PantryItem)

    @Query("UPDATE pantry_items SET isConsumed = :consumed WHERE id = :id")
    suspend fun setConsumed(id: Long, consumed: Boolean): Int
}
