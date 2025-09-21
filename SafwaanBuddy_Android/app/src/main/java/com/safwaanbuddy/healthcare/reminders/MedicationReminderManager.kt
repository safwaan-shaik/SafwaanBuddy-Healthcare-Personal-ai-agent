package com.safwaanbuddy.healthcare.reminders

import android.content.Context
import androidx.work.*
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.models.MedicationCompliance
import com.safwaanbuddy.healthcare.data.models.MedicationReminder
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Medication Reminder Manager
 * Handles scheduling and management of medication reminders using WorkManager
 */
@Singleton
class MedicationReminderManager @Inject constructor(
    private val context: Context,
    private val database: HealthcareDatabase,
    private val encryptionService: HealthcareEncryptionService,
    private val notificationHelper: NotificationHelper
) {
    
    companion object {
        private const val TAG = "MedicationReminder"
        private const val REMINDER_WORK_PREFIX = "medication_reminder_"
        private const val COMPLIANCE_CHECK_WORK = "compliance_check_work"
        private const val DAILY_SUMMARY_WORK = "daily_summary_work"
    }
    
    /**
     * Schedule medication reminders from prescription data
     */
    suspend fun scheduleRemindersFromPrescription(
        prescriptionId: Long,
        patientId: Long,
        medications: List<MedicationInfo>
    ): List<Long> = withContext(Dispatchers.IO) {
        val reminderIds = mutableListOf<Long>()
        
        medications.forEach { medication ->
            try {
                val reminderTimes = parseFrequencyToTimes(medication.frequency ?: "once daily")
                val startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val endDate = calculateEndDate(medication.duration, startDate)
                
                val reminder = MedicationReminder(
                    patientId = patientId,
                    prescriptionId = prescriptionId,
                    medicationName = medication.name,
                    dosage = "${medication.strength} ${medication.form}".trim(),
                    frequency = medication.frequency ?: "As prescribed",
                    scheduledTimes = reminderTimes.joinToString(","),
                    startDate = startDate,
                    endDate = endDate,
                    instructions = medication.instructions,
                    pregnancySafetyCategory = medication.pregnancySafetyCategory,
                    nextReminder = calculateNextReminder(reminderTimes),
                    createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                    updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                )
                
                val reminderId = database.medicationReminderDao().insertReminder(reminder)
                reminderIds.add(reminderId)
                
                // Schedule WorkManager jobs for this reminder
                scheduleReminderWork(reminderId, reminderTimes, startDate, endDate)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to schedule reminder for ${medication.name}", e)
            }
        }
        
        // Schedule daily compliance check
        scheduleDailyComplianceCheck(patientId)
        
        reminderIds
    }
    
    /**
     * Schedule individual medication reminder work
     */
    private suspend fun scheduleReminderWork(
        reminderId: Long,
        reminderTimes: List<String>,
        startDate: LocalDate,
        endDate: LocalDate?
    ) {
        reminderTimes.forEach { time ->
            val workName = "${REMINDER_WORK_PREFIX}${reminderId}_$time"
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()
            
            val inputData = workDataOf(
                "reminder_id" to reminderId,
                "reminder_time" to time,
                "start_date" to startDate.toString(),
                "end_date" to (endDate?.toString() ?: "")
            )
            
            // Calculate initial delay until first reminder
            val initialDelay = calculateInitialDelay(time, startDate)
            
            val reminderWork = PeriodicWorkRequestBuilder<MedicationReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag("medication_reminder")
                .addTag("reminder_$reminderId")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderWork
            )
        }
    }
    
    /**
     * Parse medication frequency to specific times
     */
    private fun parseFrequencyToTimes(frequency: String): List<String> {
        val freqLower = frequency.lowercase()
        
        return when {
            freqLower.contains("once") || freqLower.contains("1") && freqLower.contains("day") -> {
                listOf("08:00") // Morning
            }
            freqLower.contains("twice") || freqLower.contains("2") && freqLower.contains("day") -> {
                listOf("08:00", "20:00") // Morning and evening
            }
            freqLower.contains("thrice") || freqLower.contains("3") && freqLower.contains("day") -> {
                listOf("08:00", "14:00", "20:00") // Morning, afternoon, evening
            }
            freqLower.contains("4") && freqLower.contains("day") -> {
                listOf("08:00", "12:00", "16:00", "20:00") // Every 6 hours
            }
            freqLower.contains("every 8 hours") -> {
                listOf("08:00", "16:00", "00:00") // Every 8 hours
            }
            freqLower.contains("every 6 hours") -> {
                listOf("06:00", "12:00", "18:00", "00:00") // Every 6 hours
            }
            freqLower.contains("every 4 hours") -> {
                listOf("08:00", "12:00", "16:00", "20:00", "00:00", "04:00") // Every 4 hours
            }
            freqLower.contains("morning") -> {
                listOf("08:00")
            }
            freqLower.contains("evening") || freqLower.contains("night") -> {
                listOf("20:00")
            }
            freqLower.contains("bedtime") -> {
                listOf("22:00")
            }
            else -> {
                listOf("08:00") // Default to morning
            }
        }
    }
    
    /**
     * Calculate end date from duration string
     */
    private fun calculateEndDate(duration: String?, startDate: LocalDate): LocalDate? {
        if (duration.isNullOrBlank()) return null
        
        val durationLower = duration.lowercase()
        val numberRegex = Regex("""(\d+)""")
        val number = numberRegex.find(durationLower)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        
        return when {
            durationLower.contains("day") -> startDate.plus(number, DateTimeUnit.DAY)
            durationLower.contains("week") -> startDate.plus(number * 7, DateTimeUnit.DAY)
            durationLower.contains("month") -> startDate.plus(number, DateTimeUnit.MONTH)
            durationLower.contains("year") -> startDate.plus(number, DateTimeUnit.YEAR)
            else -> startDate.plus(30, DateTimeUnit.DAY) // Default to 30 days
        }
    }
    
    /**
     * Calculate next reminder time
     */
    private fun calculateNextReminder(reminderTimes: List<String>): LocalDateTime {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        
        // Find the next reminder time today or tomorrow
        for (timeStr in reminderTimes.sorted()) {
            val timeParts = timeStr.split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            
            val reminderTime = today.atTime(hour, minute)
            
            if (reminderTime > now) {
                return reminderTime
            }
        }
        
        // If no more reminders today, return first reminder tomorrow
        val firstTime = reminderTimes.minOrNull() ?: "08:00"
        val timeParts = firstTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        return today.plus(1, DateTimeUnit.DAY).atTime(hour, minute)
    }
    
    /**
     * Calculate initial delay for WorkManager
     */
    private fun calculateInitialDelay(time: String, startDate: LocalDate): Long {
        val timeParts = time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        val targetTime = startDate.atTime(hour, minute)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        val delay = if (targetTime > now) {
            targetTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds - 
            now.toInstant(TimeZone.currentSystemDefault()).epochSeconds
        } else {
            // Schedule for tomorrow
            val tomorrow = targetTime.date.plus(1, DateTimeUnit.DAY).atTime(hour, minute)
            tomorrow.toInstant(TimeZone.currentSystemDefault()).epochSeconds - 
            now.toInstant(TimeZone.currentSystemDefault()).epochSeconds
        }
        
        return delay * 1000 // Convert to milliseconds
    }
    
    /**
     * Handle medication taken action
     */
    suspend fun markMedicationTaken(
        reminderId: Long,
        takenAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ) {
        try {
            val reminder = database.medicationReminderDao().getReminderById(reminderId)
            if (reminder != null) {
                // Record compliance
                val compliance = MedicationCompliance(
                    medicationReminderId = reminderId,
                    patientId = reminder.patientId,
                    scheduledTime = reminder.nextReminder ?: takenAt,
                    actualTime = takenAt,
                    status = "taken",
                    delayMinutes = calculateDelayMinutes(reminder.nextReminder, takenAt),
                    createdAt = takenAt
                )
                
                database.medicationComplianceDao().insertCompliance(compliance)
                
                // Update next reminder
                val nextReminder = calculateNextReminderForMedication(reminder)
                database.medicationReminderDao().updateNextReminder(
                    reminderId,
                    nextReminder,
                    takenAt
                )
                
                // Cancel current notification
                notificationHelper.cancelMedicationReminder(reminderId.toInt())
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to mark medication taken", e)
        }
    }
    
    /**
     * Handle medication missed action
     */
    suspend fun markMedicationMissed(reminderId: Long) {
        try {
            val reminder = database.medicationReminderDao().getReminderById(reminderId)
            if (reminder != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                
                // Record missed compliance
                val compliance = MedicationCompliance(
                    medicationReminderId = reminderId,
                    patientId = reminder.patientId,
                    scheduledTime = reminder.nextReminder ?: now,
                    actualTime = null,
                    status = "missed",
                    delayMinutes = 0,
                    createdAt = now
                )
                
                database.medicationComplianceDao().insertCompliance(compliance)
                
                // Update next reminder
                val nextReminder = calculateNextReminderForMedication(reminder)
                database.medicationReminderDao().updateNextReminder(
                    reminderId,
                    nextReminder,
                    now
                )
                
                // Cancel current notification
                notificationHelper.cancelMedicationReminder(reminderId.toInt())
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to mark medication missed", e)
        }
    }
    
    /**
     * Snooze medication reminder
     */
    suspend fun snoozeMedicationReminder(reminderId: Long, snoozeMinutes: Int = 15) {
        try {
            val reminder = database.medicationReminderDao().getReminderById(reminderId)
            if (reminder != null) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val snoozeUntil = now.plus(snoozeMinutes, DateTimeUnit.MINUTE)
                
                // Update reminder with snooze time
                database.medicationReminderDao().updateNextReminder(
                    reminderId,
                    snoozeUntil,
                    now
                )
                
                // Update snooze count
                val updatedReminder = reminder.copy(
                    snoozeCount = reminder.snoozeCount + 1,
                    updatedAt = now
                )
                database.medicationReminderDao().updateReminder(updatedReminder)
                
                // Schedule snoozed notification
                notificationHelper.scheduleMedicationReminder(
                    reminderId.toInt(),
                    reminder.medicationName,
                    reminder.dosage,
                    snoozeUntil
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to snooze medication reminder", e)
        }
    }
    
    /**
     * Calculate next reminder for a specific medication
     */
    private fun calculateNextReminderForMedication(reminder: MedicationReminder): LocalDateTime {
        val reminderTimes = reminder.scheduledTimes.split(",").map { it.trim() }
        return calculateNextReminder(reminderTimes)
    }
    
    /**
     * Calculate delay in minutes between scheduled and actual time
     */
    private fun calculateDelayMinutes(scheduledTime: LocalDateTime?, actualTime: LocalDateTime): Int {
        if (scheduledTime == null) return 0
        
        val scheduledInstant = scheduledTime.toInstant(TimeZone.currentSystemDefault())
        val actualInstant = actualTime.toInstant(TimeZone.currentSystemDefault())
        
        val diffSeconds = actualInstant.epochSeconds - scheduledInstant.epochSeconds
        return (diffSeconds / 60).toInt()
    }
    
    /**
     * Schedule daily compliance check
     */
    private fun scheduleDailyComplianceCheck(patientId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val inputData = workDataOf("patient_id" to patientId)
        
        val complianceWork = PeriodicWorkRequestBuilder<ComplianceCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
            .addTag("compliance_check")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "$COMPLIANCE_CHECK_WORK$patientId",
            ExistingPeriodicWorkPolicy.REPLACE,
            complianceWork
        )
    }
    
    /**
     * Calculate delay until midnight for daily tasks
     */
    private fun calculateDelayUntilMidnight(): Long {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val midnight = now.date.plus(1, DateTimeUnit.DAY).atTime(0, 0)
        
        val delaySeconds = midnight.toInstant(TimeZone.currentSystemDefault()).epochSeconds - 
                          now.toInstant(TimeZone.currentSystemDefault()).epochSeconds
        
        return delaySeconds * 1000 // Convert to milliseconds
    }
    
    /**
     * Cancel all reminders for a patient
     */
    suspend fun cancelAllReminders(patientId: Long) {
        try {
            val reminders = database.medicationReminderDao().getActiveRemindersByPatient(patientId)
            
            reminders.collect { reminderList ->
                reminderList.forEach { reminder ->
                    cancelReminder(reminder.id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cancel reminders for patient $patientId", e)
        }
    }
    
    /**
     * Cancel specific reminder
     */
    suspend fun cancelReminder(reminderId: Long) {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            // Deactivate reminder in database
            database.medicationReminderDao().deactivateReminder(reminderId, now)
            
            // Cancel WorkManager tasks
            val reminderTimes = database.medicationReminderDao().getReminderById(reminderId)?.scheduledTimes?.split(",") ?: emptyList()
            reminderTimes.forEach { time ->
                val workName = "${REMINDER_WORK_PREFIX}${reminderId}_$time"
                WorkManager.getInstance(context).cancelUniqueWork(workName)
            }
            
            // Cancel notification
            notificationHelper.cancelMedicationReminder(reminderId.toInt())
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cancel reminder $reminderId", e)
        }
    }
    
    /**
     * Get medication compliance statistics
     */
    suspend fun getComplianceStatistics(patientId: Long, days: Int = 30): ComplianceStatistics {
        val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val startDate = endDate.minus(days, DateTimeUnit.DAY)
        
        val percentage = database.medicationComplianceDao().getCompliancePercentage(
            patientId, startDate, endDate
        ) ?: 0f
        
        val missedDoses = database.medicationComplianceDao().getRecentMissedDoses(patientId, 10)
        
        return ComplianceStatistics(
            compliancePercentage = percentage,
            totalDays = days,
            recentMissedDoses = missedDoses.size,
            lastMissedDose = missedDoses.firstOrNull()?.scheduledTime
        )
    }
}

/**
 * Data classes for medication management
 */
data class MedicationInfo(
    val name: String,
    val strength: String? = null,
    val form: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val instructions: String? = null,
    val pregnancySafetyCategory: String = "unknown"
)

data class ComplianceStatistics(
    val compliancePercentage: Float,
    val totalDays: Int,
    val recentMissedDoses: Int,
    val lastMissedDose: LocalDateTime?
)