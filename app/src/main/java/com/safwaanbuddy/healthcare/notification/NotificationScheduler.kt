package com.safwaanbuddy.healthcare.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val HEALTH_CHECK_WORK_NAME = "health_check_worker"
    
    fun scheduleHealthCheckWorker(context: Context) {
        // Constraints for the work (requires network connection)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create a periodic work request that runs once per day
        val healthCheckWork = PeriodicWorkRequestBuilder<HealthcareNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HEALTH_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthCheckWork
        )
    }
    
    fun cancelHealthCheckWorker(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(HEALTH_CHECK_WORK_NAME)
    }
}