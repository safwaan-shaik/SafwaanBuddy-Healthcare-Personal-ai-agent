package com.nurturepk.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nurturepk.data.ReminderDatabase
import com.nurturepk.utils.NotificationHelper

/**
 * WorkManager worker for handling reminder notifications
 */
class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val REMINDER_ID_KEY = "reminder_id"
        const val REMINDER_TITLE_KEY = "reminder_title"
        const val REMINDER_MESSAGE_KEY = "reminder_message"
        const val REMINDER_TYPE_KEY = "reminder_type"
    }
    
    override suspend fun doWork(): Result {
        return try {
            val reminderId = inputData.getLong(REMINDER_ID_KEY, -1)
            
            if (reminderId == -1L) {
                return Result.failure()
            }
            
            // Get reminder from database
            val database = ReminderDatabase.getDatabase(applicationContext)
            val reminder = database.reminderDao().getReminderById(reminderId)
            
            if (reminder != null && reminder.isActive) {
                // Show notification
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showReminderNotification(reminder)
                
                // If it's a recurring reminder, schedule the next occurrence
                if (reminder.isRecurring) {
                    // TODO: Implement recurring reminder logic
                    // For now, we'll just show the notification once
                }
                
                Result.success()
            } else {
                Result.failure()
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            Result.failure()
        }
    }
}