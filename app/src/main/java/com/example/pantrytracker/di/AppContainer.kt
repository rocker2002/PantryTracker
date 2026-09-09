package com.example.pantrytracker.di

import android.content.Context
import com.example.pantrytracker.data.PantryDatabase
import com.example.pantrytracker.data.PantryRepositoryImpl
import com.example.pantrytracker.domain.PantryRepository

interface AppContainer {
    val repository: PantryRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: PantryDatabase by lazy {
        PantryDatabase.getDatabase(context)
    }

    override val repository: PantryRepository by lazy {
        PantryRepositoryImpl(
            pantryDao = database.pantryDao(),
            categoryDao = database.categoryDao(),
            locationDao = database.locationDao()
        )
    }
}
