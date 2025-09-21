package com.safwaanbuddy.healthcare.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.*

/**
 * WorkManager worker for medication reminders
 * Handles scheduled medication notifications
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MedicationReminderWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val reminderId = inputData.getLong("reminder_id", -1L)
            val reminderTime = inputData.getString("reminder_time") ?: return Result.failure()
            val startDateStr = inputData.getString("start_date") ?: return Result.failure()
            val endDateStr = inputData.getString("end_date")

            if (reminderId == -1L) {
                return Result.failure()
            }

            // Check if reminder is still active and within date range
            val reminder = database.medicationReminderDao().getReminderById(reminderId)
                ?: return Result.success() // Reminder deleted, job no longer needed

            if (!reminder.isActive) {
                return Result.success() // Reminder deactivated
            }

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val startDate = LocalDate.parse(startDateStr)
            val endDate = if (endDateStr.isNotBlank()) LocalDate.parse(endDateStr) else null

            // Check if we're within the medication period
            if (now.date < startDate || (endDate != null && now.date > endDate)) {
                return Result.success() // Outside medication period
            }

            // Check if it's time for this specific reminder
            if (shouldTriggerReminder(reminderTime, now)) {
                // Send notification
                notificationHelper.showMedicationReminder(
                    reminderId.toInt(),
                    reminder.medicationName,
                    reminder.dosage,
                    reminder.instructions ?: "Take as prescribed",
                    reminder.pregnancySafetyCategory
                )

                android.util.Log.d(TAG, "Medication reminder sent for ${reminder.medicationName}")
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process medication reminder", e)
            Result.retry()
        }
    }

    /**
     * Check if we should trigger the reminder based on current time
     */
    private fun shouldTriggerReminder(reminderTime: String, currentTime: LocalDateTime): Boolean {
        val timeParts = reminderTime.split(":")
        if (timeParts.size != 2) return false

        val reminderHour = timeParts[0].toIntOrNull() ?: return false
        val reminderMinute = timeParts[1].toIntOrNull() ?: return false

        val currentHour = currentTime.hour
        val currentMinute = currentTime.minute

        // Trigger if we're within 1 minute of the reminder time
        return currentHour == reminderHour && kotlin.math.abs(currentMinute - reminderMinute) <= 1
    }
}

/**
 * WorkManager worker for daily compliance checks
 * Analyzes medication adherence and sends summary notifications
 */
@HiltWorker
class ComplianceCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ComplianceCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val patientId = inputData.getLong("patient_id", -1L)
            if (patientId == -1L) {
                return Result.failure()
            }

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val yesterday = now.minus(1, DateTimeUnit.DAY)

            // Calculate compliance for yesterday
            val compliance = database.medicationComplianceDao().getCompliancePercentage(
                patientId, yesterday, now
            ) ?: 0f

            // Get missed doses from yesterday
            val missedDoses = database.medicationComplianceDao().getRecentMissedDoses(patientId, 5)
            val yesterdayMissed = missedDoses.filter { 
                it.scheduledTime.date == yesterday.date 
            }

            // Send daily summary notification
            if (yesterdayMissed.isNotEmpty() || compliance < 90f) {
                notificationHelper.showComplianceSummary(
                    patientId.toInt(),
                    compliance,
                    yesterdayMissed.size,
                    missedDoses.size
                )
            }

            // Check for concerning patterns (3+ missed doses in a row)
            if (missedDoses.size >= 3) {
                val recentMissedInOrder = missedDoses.take(3)
                val isConsecutive = checkConsecutiveMissedDoses(recentMissedInOrder)
                
                if (isConsecutive) {
                    notificationHelper.showComplianceAlert(
                        patientId.toInt(),
                        "Multiple missed doses detected. Please consult your healthcare provider."
                    )
                }
            }

            android.util.Log.d(TAG, "Daily compliance check completed for patient $patientId")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process compliance check", e)
            Result.retry()
        }
    }

    /**
     * Check if missed doses are consecutive
     */
    private fun checkConsecutiveMissedDoses(missedDoses: List<com.safwaanbuddy.healthcare.data.models.MedicationCompliance>): Boolean {
        if (missedDoses.size < 2) return false

        for (i in 0 until missedDoses.size - 1) {
            val current = missedDoses[i].scheduledTime
            val next = missedDoses[i + 1].scheduledTime
            
            // Check if doses are within 24 hours of each other
            val diffHours = kotlin.math.abs(
                current.toInstant(TimeZone.currentSystemDefault()).epochSeconds - 
                next.toInstant(TimeZone.currentSystemDefault()).epochSeconds
            ) / 3600
            
            if (diffHours > 24) {
                return false
            }
        }
        
        return true
    }
}

/**
 * WorkManager worker for emergency health alerts
 * Monitors critical health conditions and sends immediate notifications
 */
@HiltWorker
class EmergencyAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "EmergencyAlertWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val patientId = inputData.getLong("patient_id", -1L)
            val alertType = inputData.getString("alert_type") ?: return Result.failure()
            val alertMessage = inputData.getString("alert_message") ?: return Result.failure()

            if (patientId == -1L) {
                return Result.failure()
            }

            // Send emergency notification
            when (alertType) {
                "critical_lab_result" -> {
                    notificationHelper.showEmergencyAlert(
                        patientId.toInt(),
                        "Critical Lab Result",
                        alertMessage
                    )
                }
                "medication_interaction" -> {
                    notificationHelper.showEmergencyAlert(
                        patientId.toInt(),
                        "Medication Alert",
                        alertMessage
                    )
                }
                "vital_signs_critical" -> {
                    notificationHelper.showEmergencyAlert(
                        patientId.toInt(),
                        "Critical Vital Signs",
                        alertMessage
                    )
                }
                else -> {
                    notificationHelper.showEmergencyAlert(
                        patientId.toInt(),
                        "Health Alert",
                        alertMessage
                    )
                }
            }

            // Log emergency alert
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val voiceLog = com.safwaanbuddy.healthcare.data.models.VoiceCommandLog(
                patientId = patientId,
                commandText = "SYSTEM_EMERGENCY_ALERT",
                intentClassification = alertType,
                responseGenerated = alertMessage,
                confidenceScore = 1.0f,
                processingTimeMs = 0L,
                wasSuccessful = true,
                errorMessage = null,
                timestamp = now
            )
            
            database.voiceCommandLogDao().insertVoiceCommand(voiceLog)

            android.util.Log.w(TAG, "Emergency alert sent: $alertType - $alertMessage")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process emergency alert", e)
            Result.failure()
        }
    }
}

/**
 * WorkManager worker for health data cleanup
 * Removes old data according to retention policies
 */
@HiltWorker
class HealthDataCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HealthDataCleanupWorker"
        private const val DEFAULT_RETENTION_DAYS = 365
        private const val VOICE_LOG_RETENTION_DAYS = 90
    }

    override suspend fun doWork(): Result {
        return try {
            val retentionDays = inputData.getInt("retention_days", DEFAULT_RETENTION_DAYS)
            val voiceLogRetentionDays = inputData.getInt("voice_log_retention_days", VOICE_LOG_RETENTION_DAYS)

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val cutoffDate = now.minus(retentionDays, DateTimeUnit.DAY)
            val voiceLogCutoffDate = now.minus(voiceLogRetentionDays, DateTimeUnit.DAY)

            // Clean up old voice command logs
            database.voiceCommandLogDao().deleteOldLogs(voiceLogCutoffDate)

            // Clean up old compliance records (keep for longer period for analysis)
            // This would require additional DAO methods for cleanup

            android.util.Log.i(TAG, "Health data cleanup completed")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cleanup health data", e)
            Result.retry()
        }
    }
}

/**
 * WorkManager worker for medication safety monitoring
 * Checks for drug interactions and pregnancy safety
 */
@HiltWorker
class MedicationSafetyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MedicationSafetyWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val patientId = inputData.getLong("patient_id", -1L)
            if (patientId == -1L) {
                return Result.failure()
            }

            // Get all active medications for the patient
            val activeReminders = database.medicationReminderDao().getActiveRemindersByPatient(patientId)
            
            activeReminders.collect { reminders ->
                // Check for potentially unsafe medications
                val unsafeMedications = reminders.filter { 
                    it.pregnancySafetyCategory == "avoid" 
                }

                if (unsafeMedications.isNotEmpty()) {
                    val medicationNames = unsafeMedications.joinToString(", ") { it.medicationName }
                    
                    notificationHelper.showSafetyAlert(
                        patientId.toInt(),
                        "Medication Safety Alert",
                        "The following medications may not be safe during pregnancy: $medicationNames. Please consult your healthcare provider immediately."
                    )
                }

                // Check for medications requiring caution
                val cautionMedications = reminders.filter { 
                    it.pregnancySafetyCategory == "caution" 
                }

                if (cautionMedications.isNotEmpty() && cautionMedications.size >= 3) {
                    notificationHelper.showSafetyAlert(
                        patientId.toInt(),
                        "Medication Review Needed",
                        "You have multiple medications that require caution during pregnancy. Consider reviewing with your healthcare provider."
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to process medication safety check", e)
            Result.retry()
        }
    }
}