package com.example.pantrytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PantryItem::class, Category::class, Location::class],
    version = 1,
    exportSchema = false
)
abstract class PantryDatabase : RoomDatabase() {

    abstract fun pantryDao(): PantryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: PantryDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): PantryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PantryDatabase::class.java,
                    "pantry_database"
                )
                    .addCallback(PantryDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class PantryDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database.categoryDao(), database.locationDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(
            categoryDao: CategoryDao,
            locationDao: LocationDao
        ) {
            val defaultCategories = listOf(
                Category(name = "Produce"),
                Category(name = "Dairy"),
                Category(name = "Meat & Seafood"),
                Category(name = "Bakery"),
                Category(name = "Pantry & Dry Goods"),
                Category(name = "Canned Goods"),
                Category(name = "Snacks"),
                Category(name = "Beverages"),
                Category(name = "Frozen"),
                Category(name = "Spices & Condiments")
            )
            categoryDao.insertAll(defaultCategories)

            val defaultLocations = listOf(
                Location(name = "Fridge"),
                Location(name = "Freezer"),
                Location(name = "Pantry Shelf"),
                Location(name = "Countertop"),
                Location(name = "Spice Rack")
            )
            locationDao.insertAll(defaultLocations)
        }
    }
}
