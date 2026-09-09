package com.example.pantrytracker.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.pantrytracker.PantryApplication
import com.example.pantrytracker.R
import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.domain.daysUntilExpiry

object NotificationHelper {

    const val EXPIRY_NOTIF_ID = 1001

    fun postExpiryNotification(context: Context, items: List<PantryItem>) {
        if (items.isEmpty()) return

        // 1. InboxStyle for grouped notifications (Section 6)
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle("${items.size} items expiring soon")

        items.take(5).forEach { item ->
            val daysLeft = item.daysUntilExpiry()
            val text = if (daysLeft == 0L) {
                "${item.name} — expires today"
            } else {
                "${item.name} — ${daysLeft}d left"
            }
            style.addLine(text)
        }

        if (items.size > 5) {
            style.setSummaryText("+${items.size - 5} more")
        }

        // 2. PendingIntent deep-linking into list pre-filtered to expiring items
        val pendingIntent: PendingIntent = NavDeepLinkBuilder(context)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.pantryListFragment)
            .setArguments(Bundle().apply {
                putString("filter", "EXPIRING")
            })
            .createPendingIntent()

        // 3. Build the grouped notification
        val notification = NotificationCompat.Builder(context, PantryApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pantry_alert)
            .setContentTitle("${items.size} items expiring soon")
            .setContentText(items.joinToString { it.name })
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // 4. Check runtime permission (Android 13+) before posting
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(EXPIRY_NOTIF_ID, notification)
        }
    }
}
