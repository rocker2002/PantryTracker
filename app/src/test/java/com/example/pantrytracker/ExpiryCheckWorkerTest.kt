package com.example.pantrytracker

import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.domain.ExpiryStatus
import com.example.pantrytracker.domain.expiryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryCheckWorkerTest {

    private val oneDayMillis = 24L * 60 * 60 * 1000
    private val now = 1700000000000L

    @Test
    fun workerFilter_selectsOnlyUnconsumedExpiringSoonItems() {
        val expiringSoon1 = PantryItem(
            id = 1L, name = "Milk", categoryId = 1L, locationId = 1L,
            quantity = 1f, unit = "L", purchaseDate = now,
            expiryDate = now + (1 * oneDayMillis), isConsumed = false
        )
        val expiringSoon2 = PantryItem(
            id = 2L, name = "Yogurt", categoryId = 1L, locationId = 1L,
            quantity = 500f, unit = "g", purchaseDate = now,
            expiryDate = now + (2 * oneDayMillis), isConsumed = false
        )
        val alreadyConsumedExpiringSoon = PantryItem(
            id = 3L, name = "Eggs", categoryId = 1L, locationId = 1L,
            quantity = 6f, unit = "pcs", purchaseDate = now,
            expiryDate = now + (1 * oneDayMillis), isConsumed = true
        )
        val freshItem = PantryItem(
            id = 4L, name = "Apples", categoryId = 1L, locationId = 1L,
            quantity = 1f, unit = "kg", purchaseDate = now,
            expiryDate = now + (14 * oneDayMillis), isConsumed = false
        )
        val expiredItem = PantryItem(
            id = 5L, name = "Old Bread", categoryId = 1L, locationId = 1L,
            quantity = 1f, unit = "loaf", purchaseDate = now,
            expiryDate = now - (2 * oneDayMillis), isConsumed = false
        )

        val allItems = listOf(
            expiringSoon1,
            expiringSoon2,
            alreadyConsumedExpiringSoon,
            freshItem,
            expiredItem
        )

        // The exact filtering logic executed by ExpiryCheckWorker.doWork()
        val expiringSoon = allItems.filter {
            it.expiryStatus(now) == ExpiryStatus.EXPIRING_SOON && !it.isConsumed
        }

        assertEquals(2, expiringSoon.size)
        assertTrue(expiringSoon.contains(expiringSoon1))
        assertTrue(expiringSoon.contains(expiringSoon2))
    }
}
