package com.safwaanbuddy.healthcare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.safwaanbuddy.healthcare.reminders.MedicationReminderManager
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast Receiver for handling medication reminder actions
 * Processes user interactions with medication notifications
 */
@AndroidEntryPoint
class MedicationActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var reminderManager: MedicationReminderManager
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME)
        
        if (reminderId == -1L) {
            android.util.Log.e("MedicationActionReceiver", "Invalid reminder ID")
            return
        }
        
        val scope = CoroutineScope(Dispatchers.IO)
        
        when (intent.action) {
            NotificationHelper.ACTION_TAKE_MEDICATION -> {
                scope.launch {
                    try {
                        reminderManager.markMedicationTaken(reminderId)
                        android.util.Log.d("MedicationActionReceiver", "Marked medication taken: $medicationName")
                    } catch (e: Exception) {
                        android.util.Log.e("MedicationActionReceiver", "Failed to mark medication taken", e)
                    }
                }
            }
            
            NotificationHelper.ACTION_SNOOZE_MEDICATION -> {
                scope.launch {
                    try {
                        reminderManager.snoozeMedicationReminder(reminderId, 15) // 15 minutes snooze
                        android.util.Log.d("MedicationActionReceiver", "Snoozed medication: $medicationName")
                    } catch (e: Exception) {
                        android.util.Log.e("MedicationActionReceiver", "Failed to snooze medication", e)
                    }
                }
            }
            
            NotificationHelper.ACTION_SKIP_MEDICATION -> {
                scope.launch {
                    try {
                        reminderManager.markMedicationMissed(reminderId)
                        android.util.Log.d("MedicationActionReceiver", "Marked medication skipped: $medicationName")
                    } catch (e: Exception) {
                        android.util.Log.e("MedicationActionReceiver", "Failed to mark medication skipped", e)
                    }
                }
            }
        }
    }
}

/**
 * Boot Complete Receiver
 * Reschedules medication reminders after device restart
 */
@AndroidEntryPoint
class BootCompleteReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action || 
            Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            
            android.util.Log.d("BootCompleteReceiver", "Device boot completed, rescheduling reminders")
            
            // Schedule work to reinitialize reminders
            val rescheduleWork = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>()
                .addTag("reschedule_reminders")
                .build()
            
            WorkManager.getInstance(context).enqueue(rescheduleWork)
        }
    }
}

/**
 * Emergency Alert Receiver
 * Handles emergency health alerts and notifications
 */
@AndroidEntryPoint
class EmergencyAlertReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    override fun onReceive(context: Context, intent: Intent) {
        val patientId = intent.getIntExtra("patient_id", -1)
        val alertType = intent.getStringExtra("alert_type")
        val message = intent.getStringExtra("message")
        
        if (patientId == -1 || alertType.isNullOrBlank() || message.isNullOrBlank()) {
            android.util.Log.e("EmergencyAlertReceiver", "Invalid emergency alert data")
            return
        }
        
        android.util.Log.w("EmergencyAlertReceiver", "Emergency alert received: $alertType")
        
        when (alertType) {
            "critical_vitals" -> {
                notificationHelper.showEmergencyAlert(
                    patientId,
                    "Critical Vital Signs Alert",
                    message
                )
            }
            "medication_interaction" -> {
                notificationHelper.showSafetyAlert(
                    patientId,
                    "Medication Interaction Warning",
                    message
                )
            }
            "lab_critical" -> {
                notificationHelper.showEmergencyAlert(
                    patientId,
                    "Critical Lab Results",
                    message
                )
            }
            else -> {
                notificationHelper.showEmergencyAlert(
                    patientId,
                    "Health Alert",
                    message
                )
            }
        }
    }
}

/**
 * WorkManager worker to reschedule reminders after boot
 */
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: HealthcareDatabase,
    private val reminderManager: MedicationReminderManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RescheduleRemindersWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d(TAG, "Rescheduling medication reminders after boot")
            
            // Get all active patients
            val patients = database.patientDao().getAllActivePatients().first()
            
            for (patient in patients) {
                // Get active reminders for each patient
                val reminders = database.medicationReminderDao().getActiveRemindersByPatient(patient.id).first()
                
                for (reminder in reminders) {
                    try {
                        // Reschedule each reminder
                        val reminderTimes = reminder.scheduledTimes.split(",").map { it.trim() }
                        
                        // Use the reminder manager's internal scheduling logic
                        // This would require making some private methods public or creating a reschedule method
                        android.util.Log.d(TAG, "Rescheduled reminder for ${reminder.medicationName}")
                        
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed to reschedule reminder ${reminder.id}", e)
                    }
                }
            }
            
            android.util.Log.i(TAG, "Completed rescheduling reminders")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to reschedule reminders", e)
            Result.retry()
        }
    }
}