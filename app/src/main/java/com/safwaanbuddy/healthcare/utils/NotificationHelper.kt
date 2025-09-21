package com.safwaanbuddy.healthcare.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safwaanbuddy.healthcare.MainActivity
import com.safwaanbuddy.healthcare.R
import com.safwaanbuddy.healthcare.receiver.MedicationActionReceiver
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Helper for Healthcare Application
 * Handles all medication and health-related notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // Notification Channels
        const val MEDICATION_CHANNEL_ID = "medication_reminders"
        const val EMERGENCY_CHANNEL_ID = "emergency_alerts"
        const val COMPLIANCE_CHANNEL_ID = "compliance_summary"
        const val SAFETY_CHANNEL_ID = "safety_alerts"
        
        // Notification IDs
        const val MEDICATION_REMINDER_BASE_ID = 1000
        const val EMERGENCY_ALERT_BASE_ID = 2000
        const val COMPLIANCE_SUMMARY_BASE_ID = 3000
        const val SAFETY_ALERT_BASE_ID = 4000
        
        // Action Constants
        const val ACTION_TAKE_MEDICATION = "com.safwaanbuddy.healthcare.TAKE_MEDICATION"
        const val ACTION_SNOOZE_MEDICATION = "com.safwaanbuddy.healthcare.SNOOZE_MEDICATION"
        const val ACTION_SKIP_MEDICATION = "com.safwaanbuddy.healthcare.SKIP_MEDICATION"
        
        // Extra Keys
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different types of healthcare notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    MEDICATION_CHANNEL_ID,
                    "Medication Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for medication reminders"
                    enableVibration(true)
                    setSound(null, null) // Use custom sound if needed
                },
                
                NotificationChannel(
                    EMERGENCY_CHANNEL_ID,
                    "Emergency Health Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical health alerts requiring immediate attention"
                    enableVibration(true)
                    enableLights(true)
                },
                
                NotificationChannel(
                    COMPLIANCE_CHANNEL_ID,
                    "Medication Compliance",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily medication compliance summaries"
                    enableVibration(false)
                },
                
                NotificationChannel(
                    SAFETY_CHANNEL_ID,
                    "Safety Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Medication safety and pregnancy-related alerts"
                    enableVibration(true)
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Show medication reminder notification
     */
    fun showMedicationReminder(
        reminderId: Int,
        medicationName: String,
        dosage: String,
        instructions: String,
        safetyCategory: String?
    ) {
        val notificationId = MEDICATION_REMINDER_BASE_ID + reminderId
        
        // Create main intent
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create action intents
        val takeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_TAKE_MEDICATION
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_MEDICATION
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val skipIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SKIP_MEDICATION
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val builder = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication) // You'll need to add this icon
            .setContentTitle("Time for your medication")
            .setContentText("$medicationName - $dosage")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$medicationName - $dosage\n$instructions"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_check, "Take", takePendingIntent)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_skip, "Skip", skipPendingIntent)
        
        // Add safety warning if needed
        when (safetyCategory) {
            "avoid" -> {
                builder.setColor(context.getColor(R.color.health_danger))
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("⚠️ CAUTION: This medication may not be safe during pregnancy.\n$medicationName - $dosage\n$instructions"))
            }
            "caution" -> {
                builder.setColor(context.getColor(R.color.health_caution))
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("⚠️ Use with caution during pregnancy.\n$medicationName - $dosage\n$instructions"))
            }
            "safe" -> {
                builder.setColor(context.getColor(R.color.health_safe))
            }
        }
        
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission denied for notification", e)
        }
    }
    
    /**
     * Schedule a medication reminder notification for later
     */
    fun scheduleMedicationReminder(
        reminderId: Int,
        medicationName: String,
        dosage: String,
        reminderTime: LocalDateTime
    ) {
        // This would integrate with AlarmManager for precise scheduling
        // For now, we'll use WorkManager which handles the scheduling
        android.util.Log.d("NotificationHelper", "Scheduled reminder for $medicationName at $reminderTime")
    }
    
    /**
     * Show emergency health alert
     */
    fun showEmergencyAlert(
        patientId: Int,
        title: String,
        message: String
    ) {
        val notificationId = EMERGENCY_ALERT_BASE_ID + patientId
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_emergency) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.health_danger))
            .setVibrate(longArrayOf(0, 250, 250, 250))
        
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission denied for emergency notification", e)
        }
    }
    
    /**
     * Show daily compliance summary
     */
    fun showComplianceSummary(
        patientId: Int,
        compliancePercentage: Float,
        missedYesterday: Int,
        totalRecentMissed: Int
    ) {
        val notificationId = COMPLIANCE_SUMMARY_BASE_ID + patientId
        
        val title = "Daily Medication Summary"
        val message = buildString {
            append("Compliance: ${compliancePercentage.toInt()}%")
            if (missedYesterday > 0) {
                append("\nMissed yesterday: $missedYesterday dose${if (missedYesterday > 1) "s" else ""}")
            }
            if (totalRecentMissed > 0) {
                append("\nRecent missed doses: $totalRecentMissed")
            }
        }
        
        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, COMPLIANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_compliance) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        when {
            compliancePercentage >= 90f -> builder.setColor(context.getColor(R.color.health_safe))
            compliancePercentage >= 70f -> builder.setColor(context.getColor(R.color.health_caution))
            else -> builder.setColor(context.getColor(R.color.health_danger))
        }
        
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission denied for compliance notification", e)
        }
    }
    
    /**
     * Show compliance alert for concerning patterns
     */
    fun showComplianceAlert(
        patientId: Int,
        message: String
    ) {
        val notificationId = COMPLIANCE_SUMMARY_BASE_ID + patientId + 100
        
        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, COMPLIANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning) // You'll need to add this icon
            .setContentTitle("Medication Compliance Alert")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.health_danger))
        
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission denied for compliance alert", e)
        }
    }
    
    /**
     * Show safety alert for medications or health conditions
     */
    fun showSafetyAlert(
        patientId: Int,
        title: String,
        message: String
    ) {
        val notificationId = SAFETY_ALERT_BASE_ID + patientId
        
        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_safety) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(context.getColor(R.color.health_caution))
        
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission denied for safety alert", e)
        }
    }
    
    /**
     * Cancel medication reminder notification
     */
    fun cancelMedicationReminder(reminderId: Int) {
        val notificationId = MEDICATION_REMINDER_BASE_ID + reminderId
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications for a patient
     */
    fun cancelAllNotifications(patientId: Int) {
        // Cancel different types of notifications
        notificationManager.cancel(EMERGENCY_ALERT_BASE_ID + patientId)
        notificationManager.cancel(COMPLIANCE_SUMMARY_BASE_ID + patientId)
        notificationManager.cancel(SAFETY_ALERT_BASE_ID + patientId)
        
        // Note: Medication reminders would need to be cancelled individually
        // as they use different IDs based on reminder ID
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    fun shouldRequestNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !areNotificationsEnabled()
    }
}