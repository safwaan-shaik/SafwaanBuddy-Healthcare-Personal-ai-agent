package com.nurturepk.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nurturepk.MainActivity
import com.nurturepk.R
import com.nurturepk.data.Reminder
import com.nurturepk.data.ReminderType

/**
 * Helper class for managing notifications in the NurturePK app
 */
class NotificationHelper(private val context: Context) {
    
    companion object {
        const val MEDICATION_CHANNEL_ID = "medication_reminders"
        const val NUTRITION_CHANNEL_ID = "nutrition_reminders"
        const val ACTIVITY_CHANNEL_ID = "activity_reminders"
        
        private const val MEDICATION_CHANNEL_NAME = "Medication Reminders"
        private const val NUTRITION_CHANNEL_NAME = "Nutrition Reminders"
        private const val ACTIVITY_CHANNEL_NAME = "Activity Reminders"
        
        private const val MEDICATION_CHANNEL_DESCRIPTION = "Notifications for medication reminders"
        private const val NUTRITION_CHANNEL_DESCRIPTION = "Notifications for nutrition reminders"
        private const val ACTIVITY_CHANNEL_DESCRIPTION = "Notifications for activity reminders"
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Creates notification channels for different reminder types
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Medication Channel - High Priority
            val medicationChannel = NotificationChannel(
                MEDICATION_CHANNEL_ID,
                MEDICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = MEDICATION_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            
            // Nutrition Channel - Default Priority
            val nutritionChannel = NotificationChannel(
                NUTRITION_CHANNEL_ID,
                NUTRITION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = NUTRITION_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Activity Channel - Default Priority
            val activityChannel = NotificationChannel(
                ACTIVITY_CHANNEL_ID,
                ACTIVITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ACTIVITY_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(medicationChannel, nutritionChannel, activityChannel)
            )
        }
    }
    
    /**
     * Displays a notification for a reminder
     */
    fun showReminderNotification(reminder: Reminder) {
        val channelId = when (reminder.reminderType) {
            ReminderType.MEDICATION -> MEDICATION_CHANNEL_ID
            ReminderType.NUTRITION -> NUTRITION_CHANNEL_ID
            ReminderType.ACTIVITY -> ACTIVITY_CHANNEL_ID
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(reminder.reminderType))
            .setContentTitle(reminder.title)
            .setContentText(reminder.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.message))
            .setPriority(getNotificationPriority(reminder.reminderType))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(reminder.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Handle permission denial gracefully
            e.printStackTrace()
        }
    }
    
    /**
     * Cancels a notification for a specific reminder
     */
    fun cancelNotification(reminderId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(reminderId.toInt())
    }
    
    /**
     * Gets appropriate notification icon based on reminder type
     */
    private fun getNotificationIcon(type: ReminderType): Int {
        return when (type) {
            ReminderType.MEDICATION -> android.R.drawable.ic_menu_add // Placeholder
            ReminderType.NUTRITION -> android.R.drawable.ic_menu_add // Placeholder
            ReminderType.ACTIVITY -> android.R.drawable.ic_menu_add // Placeholder
        }
    }
    
    /**
     * Gets notification priority based on reminder type
     */
    private fun getNotificationPriority(type: ReminderType): Int {
        return when (type) {
            ReminderType.MEDICATION -> NotificationCompat.PRIORITY_HIGH
            ReminderType.NUTRITION -> NotificationCompat.PRIORITY_DEFAULT
            ReminderType.ACTIVITY -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
    
    /**
     * Checks if notifications are enabled for the app
     */
    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Creates a test notification to verify setup
     */
    fun showTestNotification() {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("NurturePK Test")
            .setContentText("Notification system is working!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(9999, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}