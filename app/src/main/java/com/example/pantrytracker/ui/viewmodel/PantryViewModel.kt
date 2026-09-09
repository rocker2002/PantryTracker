package com.example.pantrytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pantrytracker.data.Category
import com.example.pantrytracker.data.Location
import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.domain.ExpiryStatus
import com.example.pantrytracker.domain.PantryRepository
import com.example.pantrytracker.domain.expiryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ExpiryFilter {
    ALL,
    EXPIRING_SOON,
    FRESH,
    EXPIRED
}

data class PantryUiState(
    val items: List<PantryItem> = emptyList(),
    val totalActiveCount: Int = 0,
    val expiringCount: Int = 0,
    val categories: Map<Long, String> = emptyMap(),
    val locations: Map<Long, String> = emptyMap(),
    val allLocationsList: List<Location> = emptyList(),
    val expiryFilter: ExpiryFilter = ExpiryFilter.ALL,
    val selectedLocationId: Long? = null,
    val isLoading: Boolean = false
)

class PantryViewModel(
    private val repository: PantryRepository
) : ViewModel() {

    private val _expiryFilter = MutableStateFlow(ExpiryFilter.ALL)
    private val _locationFilter = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PantryUiState> = combine(
        repository.getActiveItems(),
        repository.getAllCategories(),
        repository.getAllLocations(),
        _expiryFilter,
        _locationFilter
    ) { activeItems, categoriesList, locationsList, expiryFilter, locationFilterId ->
        val now = System.currentTimeMillis()
        val expiringCount = activeItems.count { it.expiryStatus(now) == ExpiryStatus.EXPIRING_SOON }

        val categoryMap = categoriesList.associate { it.id to it.name }
        val locationMap = locationsList.associate { it.id to it.name }

        // Filter items
        val filteredItems = activeItems.filter { item ->
            val matchesExpiry = when (expiryFilter) {
                ExpiryFilter.ALL -> true
                ExpiryFilter.EXPIRING_SOON -> item.expiryStatus(now) == ExpiryStatus.EXPIRING_SOON
                ExpiryFilter.FRESH -> item.expiryStatus(now) == ExpiryStatus.FRESH
                ExpiryFilter.EXPIRED -> item.expiryStatus(now) == ExpiryStatus.EXPIRED
            }

            val matchesLocation = locationFilterId == null || item.locationId == locationFilterId
            matchesExpiry && matchesLocation
        }

        PantryUiState(
            items = filteredItems,
            totalActiveCount = activeItems.size,
            expiringCount = expiringCount,
            categories = categoryMap,
            locations = locationMap,
            allLocationsList = locationsList,
            expiryFilter = expiryFilter,
            selectedLocationId = locationFilterId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PantryUiState(isLoading = true)
    )

    fun setExpiryFilter(filter: ExpiryFilter) {
        _expiryFilter.value = filter
    }

    fun setLocationFilter(locationId: Long?) {
        _locationFilter.value = locationId
    }

    fun markConsumed(item: PantryItem) {
        viewModelScope.launch {
            repository.setItemConsumed(item.id, true)
        }
    }

    fun undoConsumed(item: PantryItem) {
        viewModelScope.launch {
            repository.setItemConsumed(item.id, false)
        }
    }

    val allCategories: Flow<List<Category>> = repository.getAllCategories()
    val allLocations: Flow<List<Location>> = repository.getAllLocations()

    fun addItem(
        name: String,
        categoryId: Long,
        locationId: Long,
        quantity: Float,
        unit: String,
        purchaseDate: Long = System.currentTimeMillis(),
        expiryDate: Long,
        barcode: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val item = PantryItem(
                name = name,
                categoryId = categoryId,
                locationId = locationId,
                quantity = quantity,
                unit = unit,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                barcode = barcode,
                isConsumed = false
            )
            repository.insertItem(item)
            onSuccess?.invoke()
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
}

class PantryViewModelFactory(
    private val repository: PantryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PantryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
