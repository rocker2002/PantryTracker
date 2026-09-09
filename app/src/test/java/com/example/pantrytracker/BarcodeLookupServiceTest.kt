package com.example.pantrytracker

import com.example.pantrytracker.data.scanner.BarcodeLookupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeLookupServiceTest {

    private val service = BarcodeLookupService()

    @Test
    fun parseProductJson_dairyProduct_correctlyCategorizedAndMapped() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Organic Whole Milk",
                "categories_tags": ["en:dairies", "en:milks"],
                "quantity": "1 L",
                "image_front_url": "https://example.com/milk.jpg"
            }
        }
        """.trimIndent()

        val product = service.parseProductJson("8961234567890", json)

        assertNotNull(product)
        assertEquals("Organic Whole Milk", product?.name)
        assertEquals("Dairy", product?.categorySuggestion)
        assertEquals("Fridge", product?.locationSuggestion)
        assertEquals(1.0, product?.quantity)
        assertEquals("L", product?.unit)
        assertEquals("https://example.com/milk.jpg", product?.imageUrl)
    }

    @Test
    fun parseProductJson_bakeryProduct_correctlyCategorized() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name_en": "Multigrain Sliced Bread",
                "categories_tags": ["en:breads", "en:bakery-products"],
                "quantity": "400 g"
            }
        }
        """.trimIndent()

        val product = service.parseProductJson("1234567890", json)

        assertNotNull(product)
        assertEquals("Multigrain Sliced Bread", product?.name)
        assertEquals("Bakery", product?.categorySuggestion)
        assertEquals("Pantry Shelf", product?.locationSuggestion)
        assertEquals(400.0, product?.quantity)
        assertEquals("g", product?.unit)
    }

    @Test
    fun parseProductJson_frozenProduct_correctlyCategorizedToFreezer() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Vanilla Ice Cream",
                "categories_tags": ["en:desserts", "en:frozen-foods", "en:ice-creams"],
                "quantity": "500 ml"
            }
        }
        """.trimIndent()

        val product = service.parseProductJson("9876543210", json)

        assertNotNull(product)
        assertEquals("Vanilla Ice Cream", product?.name)
        assertEquals("Frozen", product?.categorySuggestion)
        assertEquals("Freezer", product?.locationSuggestion)
        assertEquals(500.0, product?.quantity)
        assertEquals("ml", product?.unit)
    }

    @Test
    fun parseProductJson_statusZero_returnsNull() {
        val json = """
        {
            "status": 0,
            "status_verbose": "product not found"
        }
        """.trimIndent()

        val product = service.parseProductJson("0000000000", json)
        assertNull(product)
    }
}