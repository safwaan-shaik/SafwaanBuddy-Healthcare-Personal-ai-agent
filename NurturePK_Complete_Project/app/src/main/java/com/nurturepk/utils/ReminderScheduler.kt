package com.nurturepk.utils

import android.content.Context
import androidx.work.*
import com.nurturepk.data.Reminder
import com.nurturepk.worker.ReminderWorker
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Scheduler for managing reminder notifications using WorkManager
 */
class ReminderScheduler(private val context: Context) {
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Schedules a notification for a reminder
     */
    fun scheduleReminder(reminder: Reminder) {
        val currentTime = Clock.System.now()
        val reminderInstant = reminder.scheduledTime.toInstant(TimeZone.currentSystemDefault())
        
        // Only schedule if the reminder is in the future
        if (reminderInstant > currentTime) {
            val delay = reminderInstant.toEpochMilliseconds() - currentTime.toEpochMilliseconds()
            
            val inputData = Data.Builder()
                .putLong(ReminderWorker.REMINDER_ID_KEY, reminder.id)
                .putString(ReminderWorker.REMINDER_TITLE_KEY, reminder.title)
                .putString(ReminderWorker.REMINDER_MESSAGE_KEY, reminder.message)
                .putString(ReminderWorker.REMINDER_TYPE_KEY, reminder.reminderType.name)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(getWorkTag(reminder.id))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .build()
            
            workManager.enqueue(workRequest)
        }
    }
    
    /**
     * Schedules a recurring reminder
     */
    fun scheduleRecurringReminder(reminder: Reminder, intervalHours: Long = 24) {
        if (!reminder.isRecurring) return
        
        val inputData = Data.Builder()
            .putLong(ReminderWorker.REMINDER_ID_KEY, reminder.id)
            .putString(ReminderWorker.REMINDER_TITLE_KEY, reminder.title)
            .putString(ReminderWorker.REMINDER_MESSAGE_KEY, reminder.message)
            .putString(ReminderWorker.REMINDER_TYPE_KEY, reminder.reminderType.name)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setInputData(inputData)
            .addTag(getWorkTag(reminder.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * Cancels a scheduled reminder
     */
    fun cancelReminder(reminderId: Long) {
        workManager.cancelAllWorkByTag(getWorkTag(reminderId))
    }
    
    /**
     * Cancels all scheduled reminders
     */
    fun cancelAllReminders() {
        workManager.cancelAllWork()
    }
    
    /**
     * Reschedules all active reminders (useful after device reboot)
     */
    suspend fun rescheduleAllReminders(reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (reminder.isActive) {
                if (reminder.isRecurring) {
                    scheduleRecurringReminder(reminder)
                } else {
                    scheduleReminder(reminder)
                }
            }
        }
    }
    
    /**
     * Gets the work tag for a reminder
     */
    private fun getWorkTag(reminderId: Long): String {
        return "reminder_$reminderId"
    }
    
    /**
     * Checks if a reminder is currently scheduled
     */
    fun isReminderScheduled(reminderId: Long): Boolean {
        val workInfos = workManager.getWorkInfosByTag(getWorkTag(reminderId))
        return try {
            val workInfoList = workInfos.get()
            workInfoList.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets all scheduled work info for debugging
     */
    fun getAllScheduledWork(): List<WorkInfo> {
        return try {
            workManager.getWorkInfosByTag("reminder_").get()
        } catch (e: Exception) {
            emptyList()
        }
    }
}