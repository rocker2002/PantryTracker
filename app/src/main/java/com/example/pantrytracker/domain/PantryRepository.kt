package com.example.pantrytracker.domain

import com.example.pantrytracker.data.Category
import com.example.pantrytracker.data.Location
import com.example.pantrytracker.data.PantryItem
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract defining the single source of truth for pantry data.
 * Both the UI (PantryViewModel) and background jobs (ExpiryCheckWorker) read through this.
 */
interface PantryRepository {
    fun getAllItems(): Flow<List<PantryItem>>
    fun getActiveItems(): Flow<List<PantryItem>>
    fun getItemById(id: Long): Flow<PantryItem?>
    suspend fun getItemByIdOnce(id: Long): PantryItem?
    suspend fun insertItem(item: PantryItem): Long
    suspend fun updateItem(item: PantryItem)
    suspend fun deleteItem(item: PantryItem)
    suspend fun setItemConsumed(id: Long, consumed: Boolean): Int

    fun getAllCategories(): Flow<List<Category>>
    fun getAllLocations(): Flow<List<Location>>
    suspend fun insertCategory(category: Category): Long
    suspend fun insertLocation(location: Location): Long
}
