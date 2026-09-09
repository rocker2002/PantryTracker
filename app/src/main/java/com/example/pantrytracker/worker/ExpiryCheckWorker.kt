package com.example.pantrytracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.pantrytracker.PantryApplication
import com.example.pantrytracker.domain.ExpiryStatus
import com.example.pantrytracker.domain.expiryStatus
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PantryApplication
        val repository = app.container.repository
        val now = System.currentTimeMillis()

        val expiringSoon = repository.getAllItems()
            .first()
            .filter { it.expiryStatus(now) == ExpiryStatus.EXPIRING_SOON && !it.isConsumed }

        if (expiringSoon.isNotEmpty()) {
            NotificationHelper.postExpiryNotification(applicationContext, expiringSoon)
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "ExpiryCheckWork"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
