package com.example.pantrytracker.data.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class ScannedProduct(
    val barcode: String,
    val name: String,
    val categorySuggestion: String?,
    val locationSuggestion: String?,
    val quantity: Double?,
    val unit: String?,
    val imageUrl: String?
)

class BarcodeLookupService {

    suspend fun lookupBarcode(barcode: String): Result<ScannedProduct?> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "PantryTracker - Android - Version 1.0")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                Result.success(parseProductJson(barcode, response))
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Result.success(null)
            } else {
                Result.failure(Exception("HTTP error $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseProductJson(barcode: String, jsonString: String): ScannedProduct? {
        val root = JSONObject(jsonString)
        val status = root.optInt("status", 0)
        if (status != 1) return null

        val product = root.optJSONObject("product") ?: return null

        // 1. Resolve Product Name
        val name = product.optString("product_name").ifEmpty {
            product.optString("product_name_en").ifEmpty {
                product.optString("generic_name").ifEmpty {
                    product.optString("generic_name_en")
                }
            }
        }.trim()

        if (name.isEmpty()) return null

        // 2. Resolve Category
        val categoriesTags = mutableListOf<String>()
        val tagsArray = product.optJSONArray("categories_tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                categoriesTags.add(tagsArray.optString(i).lowercase())
            }
        }
        val categoriesStr = product.optString("categories").lowercase()
        val allCategoryInfo = (categoriesTags.joinToString(" ") + " " + categoriesStr).lowercase()

        val categorySuggestion = mapCategory(allCategoryInfo)
        val locationSuggestion = mapLocation(categorySuggestion)

        // 3. Resolve Quantity and Unit
        val quantityStr = product.optString("quantity").trim()
        val (parsedQty, parsedUnit) = parseQuantityAndUnit(quantityStr)

        // 4. Resolve Image URL
        val imageUrl = product.optString("image_front_url").ifEmpty {
            product.optString("image_url")
        }.ifEmpty { null }

        return ScannedProduct(
            barcode = barcode,
            name = name,
            categorySuggestion = categorySuggestion,
            locationSuggestion = locationSuggestion,
            quantity = parsedQty,
            unit = parsedUnit,
            imageUrl = imageUrl
        )
    }

    private fun mapCategory(info: String): String {
        return when {
            containsAny(info, "frozen", "ice cream", "ice-cream", "glace", "surge") -> "Frozen"
            containsAny(info, "dair", "milk", "cheese", "yogurt", "butter", "cream", "fromage") -> "Dairy"
            containsAny(info, "fruit", "vegetable", "produce", "salad", "fresh-plant", "legume") -> "Produce"
            containsAny(info, "bread", "bakery", "biscuit", "cake", "pastr", "toast", "croissant") -> "Bakery"
            containsAny(info, "meat", "poultry", "beef", "chicken", "fish", "seafood", "pork", "viande") -> "Meat & Seafood"
            containsAny(info, "canned", "tin", "preserves", "conserves") -> "Canned Goods"
            containsAny(info, "beverage", "drink", "juice", "tea", "coffee", "soda", "water", "boisson") -> "Beverages"
            containsAny(info, "snack", "chip", "cracker", "chocolate", "candy", "cookie", "crisp") -> "Snacks"
            containsAny(info, "condiment", "sauce", "spice", "oil", "vinegar", "mustard", "ketchup") -> "Condiments & Spices"
            else -> "Pantry Shelf"
        }
    }

    private fun mapLocation(category: String): String {
        return when (category) {
            "Dairy", "Meat & Seafood", "Produce" -> "Fridge"
            "Frozen" -> "Freezer"
            else -> "Pantry Shelf"
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun parseQuantityAndUnit(quantityStr: String): Pair<Double?, String?> {
        if (quantityStr.isEmpty()) return Pair(null, null)

        val pattern = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z]+)?.*$")
        val matcher = pattern.matcher(quantityStr)
        if (matcher.find()) {
            val qty = matcher.group(1)?.toDoubleOrNull()
            val rawUnit = matcher.group(2)?.lowercase()
            val unit = when (rawUnit) {
                "g", "gram", "grams" -> "g"
                "kg", "kilo", "kilogram" -> "kg"
                "ml", "milliliter" -> "ml"
                "l", "liter", "litres" -> "L"
                "pcs", "piece", "pieces" -> "pcs"
                "pack", "pk" -> "pack"
                else -> null
            }
            return Pair(qty, unit)
        }
        return Pair(null, null)
    }
}