package com.example.pantrytracker

import com.example.pantrytracker.data.Category
import com.example.pantrytracker.data.Location
import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.domain.PantryRepository
import com.example.pantrytracker.ui.viewmodel.ExpiryFilter
import com.example.pantrytracker.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val oneDayMillis = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()

    private val fakeItemsFlow = MutableStateFlow<List<PantryItem>>(emptyList())
    private val fakeCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val fakeLocationsFlow = MutableStateFlow<List<Location>>(emptyList())

    private val fakeRepository = object : PantryRepository {
        override fun getAllItems(): Flow<List<PantryItem>> = fakeItemsFlow
        override fun getActiveItems(): Flow<List<PantryItem>> = fakeItemsFlow
        override fun getItemById(id: Long): Flow<PantryItem?> = MutableStateFlow(null)
        override suspend fun getItemByIdOnce(id: Long): PantryItem? = null
        override suspend fun insertItem(item: PantryItem): Long = 1L
        override suspend fun updateItem(item: PantryItem) {}
        override suspend fun deleteItem(item: PantryItem) {}
        override suspend fun setItemConsumed(id: Long, consumed: Boolean): Int = 1
        override fun getAllCategories(): Flow<List<Category>> = fakeCategoriesFlow
        override fun getAllLocations(): Flow<List<Location>> = fakeLocationsFlow
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun insertLocation(location: Location): Long = 1L
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_calculatesExpiringCountCorrectly() = runTest {
        val itemFresh = PantryItem(
            id = 1L, name = "Apples", categoryId = 1L, locationId = 1L,
            quantity = 2f, unit = "kg", purchaseDate = now, expiryDate = now + (10 * oneDayMillis)
        )
        val itemExpiringSoon = PantryItem(
            id = 2L, name = "Milk", categoryId = 2L, locationId = 1L,
            quantity = 1f, unit = "L", purchaseDate = now, expiryDate = now + (1 * oneDayMillis)
        )
        fakeItemsFlow.value = listOf(itemFresh, itemExpiringSoon)
        fakeCategoriesFlow.value = listOf(Category(1L, "Produce"), Category(2L, "Dairy"))
        fakeLocationsFlow.value = listOf(Location(1L, "Fridge"))

        val viewModel = PantryViewModel(fakeRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(1, state.expiringCount)
    }

    @Test
    fun uiState_filtersByExpiringSoon() = runTest {
        val itemFresh = PantryItem(
            id = 1L, name = "Apples", categoryId = 1L, locationId = 1L,
            quantity = 2f, unit = "kg", purchaseDate = now, expiryDate = now + (10 * oneDayMillis)
        )
        val itemExpiringSoon = PantryItem(
            id = 2L, name = "Milk", categoryId = 2L, locationId = 1L,
            quantity = 1f, unit = "L", purchaseDate = now, expiryDate = now + (1 * oneDayMillis)
        )
        fakeItemsFlow.value = listOf(itemFresh, itemExpiringSoon)

        val viewModel = PantryViewModel(fakeRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.setExpiryFilter(ExpiryFilter.EXPIRING_SOON)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("Milk", state.items[0].name)
    }
}
