package com.example.pantrytracker

import com.example.pantrytracker.data.Category
import com.example.pantrytracker.data.Location
import com.example.pantrytracker.data.PantryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun category_creation_hasExpectedDefaults() {
        val category = Category(name = "Produce")
        assertEquals(0L, category.id)
        assertEquals("Produce", category.name)
    }

    @Test
    fun location_creation_hasExpectedDefaults() {
        val location = Location(name = "Fridge")
        assertEquals(0L, location.id)
        assertEquals("Fridge", location.name)
    }

    @Test
    fun pantryItem_creation_hasExpectedDefaults() {
        val item = PantryItem(
            name = "Milk",
            categoryId = 1L,
            locationId = 2L,
            quantity = 1.0f,
            unit = "liters",
            purchaseDate = 1000L,
            expiryDate = 5000L
        )
        assertEquals(0L, item.id)
        assertEquals("Milk", item.name)
        assertEquals(1L, item.categoryId)
        assertEquals(2L, item.locationId)
        assertEquals(1.0f, item.quantity, 0.001f)
        assertEquals("liters", item.unit)
        assertEquals(1000L, item.purchaseDate)
        assertEquals(5000L, item.expiryDate)
        assertNull(item.barcode)
        assertFalse(item.isConsumed)
    }
}