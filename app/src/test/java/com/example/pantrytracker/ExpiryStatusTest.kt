package com.example.pantrytracker

import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.domain.ExpiryStatus
import com.example.pantrytracker.domain.daysUntilExpiry
import com.example.pantrytracker.domain.expiryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryStatusTest {

    private val oneDayMillis = 24L * 60 * 60 * 1000
    private val baseTime = 1700000000000L // arbitrary fixed epoch timestamp

    private fun createItemWithExpiry(expiryDate: Long): PantryItem {
        return PantryItem(
            name = "Test Item",
            categoryId = 1L,
            locationId = 1L,
            quantity = 1f,
            unit = "pcs",
            purchaseDate = baseTime - (10 * oneDayMillis),
            expiryDate = expiryDate
        )
    }

    @Test
    fun itemExpiredInPast_returnsExpiredStatus() {
        val item = createItemWithExpiry(baseTime - (2 * oneDayMillis))
        assertEquals(-2L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.EXPIRED, item.expiryStatus(baseTime))
    }

    @Test
    fun itemExpiringToday_returnsExpiringSoonStatus() {
        val item = createItemWithExpiry(baseTime)
        assertEquals(0L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.EXPIRING_SOON, item.expiryStatus(baseTime))
    }

    @Test
    fun itemExpiringInOneDay_returnsExpiringSoonStatus() {
        val item = createItemWithExpiry(baseTime + (1 * oneDayMillis))
        assertEquals(1L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.EXPIRING_SOON, item.expiryStatus(baseTime))
    }

    @Test
    fun itemExpiringInThreeDays_boundaryReturnsExpiringSoonStatus() {
        val item = createItemWithExpiry(baseTime + (3 * oneDayMillis))
        assertEquals(3L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.EXPIRING_SOON, item.expiryStatus(baseTime))
    }

    @Test
    fun itemExpiringInFourDays_returnsFreshStatus() {
        val item = createItemWithExpiry(baseTime + (4 * oneDayMillis))
        assertEquals(4L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.FRESH, item.expiryStatus(baseTime))
    }

    @Test
    fun itemExpiringInThirtyDays_returnsFreshStatus() {
        val item = createItemWithExpiry(baseTime + (30 * oneDayMillis))
        assertEquals(30L, item.daysUntilExpiry(baseTime))
        assertEquals(ExpiryStatus.FRESH, item.expiryStatus(baseTime))
    }
}
