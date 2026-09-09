package com.example.pantrytracker.data

import com.example.pantrytracker.domain.PantryRepository
import kotlinx.coroutines.flow.Flow

class PantryRepositoryImpl(
    private val pantryDao: PantryDao,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao
) : PantryRepository {

    override fun getAllItems(): Flow<List<PantryItem>> = pantryDao.getAllItems()

    override fun getActiveItems(): Flow<List<PantryItem>> = pantryDao.getActiveItems()

    override fun getItemById(id: Long): Flow<PantryItem?> = pantryDao.getItemById(id)

    override suspend fun getItemByIdOnce(id: Long): PantryItem? = pantryDao.getItemByIdOnce(id)

    override suspend fun insertItem(item: PantryItem): Long = pantryDao.insert(item)

    override suspend fun updateItem(item: PantryItem) = pantryDao.update(item)

    override suspend fun deleteItem(item: PantryItem) = pantryDao.delete(item)

    override suspend fun setItemConsumed(id: Long, consumed: Boolean): Int =
        pantryDao.setConsumed(id, consumed)

    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override fun getAllLocations(): Flow<List<Location>> = locationDao.getAllLocations()

    override suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)

    override suspend fun insertLocation(location: Location): Long = locationDao.insert(location)
}
