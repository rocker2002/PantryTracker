package com.example.pantrytracker.domain

import com.example.pantrytracker.data.PantryItem

enum class ExpiryStatus {
    FRESH,
    EXPIRING_SOON,
    EXPIRED
}

private const val MILLIS_IN_A_DAY = 24L * 60 * 60 * 1000

/**
 * Calculates the number of days remaining until the item expires.
 * Can be negative if the item has already expired.
 */
fun PantryItem.daysUntilExpiry(today: Long = System.currentTimeMillis()): Long {
    return (expiryDate - today) / MILLIS_IN_A_DAY
}

/**
 * Derives the freshness status of an item relative to a target timestamp (defaulting to now).
 *
 * Rules (from system design spec):
 * - daysLeft < 0  -> EXPIRED
 * - daysLeft <= 3 -> EXPIRING_SOON
 * - else          -> FRESH
 */
fun PantryItem.expiryStatus(today: Long = System.currentTimeMillis()): ExpiryStatus {
    val daysLeft = daysUntilExpiry(today)
    return when {
        daysLeft < 0 -> ExpiryStatus.EXPIRED
        daysLeft <= 3 -> ExpiryStatus.EXPIRING_SOON
        else -> ExpiryStatus.FRESH
    }
}
