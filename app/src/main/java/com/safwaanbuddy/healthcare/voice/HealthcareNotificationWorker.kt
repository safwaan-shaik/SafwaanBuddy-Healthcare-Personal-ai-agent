// Healthcare Notification Worker
// Background worker for sending caring notifications

package com.safwaanbuddy.healthcare.voice

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import java.util.*

@HiltWorker
class HealthcareNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthcareAiService: HealthcareAiService,
    private val database: HealthcareDatabase
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "HealthcareNotificationWorker"
        const val WORK_NAME = "healthcare_notification_worker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Get the current patient
            val patientId = inputData.getLong("patient_id", -1)
            if (patientId == -1L) {
                return Result.failure()
            }
            
            // Get the notification type
            val notificationType = inputData.getString("notification_type") ?: "motivational"
            
            // Generate the appropriate message
            val message = when (notificationType) {
                "motivational" -> generateMotivationalMessage(patientId)
                "medication_reminder" -> generateMedicationReminder(patientId)
                "follow_up" -> generateFollowUpQuestion(patientId)
                "health_check" -> generateHealthCheckMessage(patientId)
                else -> generateGeneralMessage(patientId)
            }
            
            // Show a notification with the message
            NotificationHelper.showHealthcareNotification(applicationContext, message, notificationType)
            
            // Log the interaction
            logNotificationInteraction(patientId, notificationType, message)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Healthcare Notification Worker", e)
            Result.failure()
        }
    }
    
    /**
     * Generate a motivational message
     * @param patientId The ID of the current patient
     * @return The motivational message
     */
    private suspend fun generateMotivationalMessage(patientId: Long): String {
        return try {
            // Use AI to generate a personalized motivational message
            try {
                val aiMessage = healthcareAiService.generateMotivationalMessage(patientId).get()
                if (aiMessage.isNotEmpty()) {
                    aiMessage
                } else {
                    "You're doing great! Keep up the good work with your health journey."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI motivational message", e)
                "You're doing great! Keep up the good work with your health journey."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating motivational message", e)
            "You're doing great! Keep up the good work with your health journey."
        }
    }
    
    /**
     * Generate a medication reminder
     * @param patientId The ID of the current patient
     * @return The medication reminder message
     */
    private suspend fun generateMedicationReminder(patientId: Long): String {
        return try {
            // Get patient's medications
            val medications = database.medicationReminderDao().getActiveRemindersByPatient(patientId).first()
            
            if (medications.isNotEmpty()) {
                val nextMedication = medications.firstOrNull()
                if (nextMedication != null) {
                    "Don't forget to take your ${nextMedication.medicationName}! You're doing a great job staying on track."
                } else {
                    "Time for your medication! You're doing a great job staying on track."
                }
            } else {
                "Time for your medication! You're doing a great job staying on track."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating medication reminder", e)
            "Time for your medication! You're doing a great job staying on track."
        }
    }
    
    /**
     * Generate a follow-up question
     * @param patientId The ID of the current patient
     * @return The follow-up question
     */
    private suspend fun generateFollowUpQuestion(patientId: Long): String {
        return try {
            // Get the last voice command for context
            val lastCommand = database.voiceCommandLogDao().getRecentVoiceCommands(patientId, 1).firstOrNull()
            
            if (lastCommand != null) {
                // Use AI to generate a personalized follow-up question
                try {
                    val aiMessage = healthcareAiService.generateFollowUpQuestion(patientId, lastCommand.commandText).get()
                    if (aiMessage.isNotEmpty()) {
                        aiMessage
                    } else {
                        "How are you feeling today? Is there anything specific you'd like to talk about?"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating AI follow-up question", e)
                    "How are you feeling today? Is there anything specific you'd like to talk about?"
                }
            } else {
                "How are you feeling today? Is there anything specific you'd like to talk about?"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating follow-up question", e)
            "How are you feeling today? Is there anything specific you'd like to talk about?"
        }
    }
    
    /**
     * Generate a health check message
     * @param patientId The ID of the current patient
     * @return The health check message
     */
    private suspend fun generateHealthCheckMessage(patientId: Long): String {
        return try {
            // Get patient information
            val patient = database.patientDao().getPatientById(patientId)
            
            // Generate a personalized health check message
            val healthCheckMessage = if (patient != null) {
                "Hey ${patient.name}, how's your health journey going? Remember to stay hydrated and get plenty of rest!"
            } else {
                "Hey there! How's your health journey going? Remember to stay hydrated and get plenty of rest!"
            }
            
            // Use AI to generate a more personalized message
            try {
                val aiMessage = healthcareAiService.generateProactiveCheckIn(patientId).get()
                if (aiMessage.isNotEmpty()) {
                    aiMessage
                } else {
                    healthCheckMessage
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI health check message", e)
                healthCheckMessage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating health check message", e)
            "Hey there! How's your health journey going? Remember to stay hydrated and get plenty of rest!"
        }
    }
    
    /**
     * Generate a general message
     * @param patientId The ID of the current patient
     * @return The general message
     */
    private suspend fun generateGeneralMessage(patientId: Long): String {
        return try {
            // Use AI to generate a personalized general message
            try {
                val aiMessage = healthcareAiService.generateProactiveCheckIn(patientId).get()
                if (aiMessage.isNotEmpty()) {
                    aiMessage
                } else {
                    "Just checking in to see how you're doing today!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI general message", e)
                "Just checking in to see how you're doing today!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating general message", e)
            "Just checking in to see how you're doing today!"
        }
    }
    
    /**
     * Log a notification interaction
     * @param patientId The ID of the patient
     * @param interactionType The type of interaction
     * @param message The message content
     */
    private suspend fun logNotificationInteraction(patientId: Long, interactionType: String, message: String) {
        try {
            val voiceCommand = VoiceCommandLog(
                commandText = "NOTIFICATION_$interactionType",
                intentClassification = interactionType,
                responseGenerated = message,
                confidenceScore = 1.0f,
                processingTimeMs = 0L,
                wasSuccessful = true,
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                patientId = patientId
            )
            
            database.voiceCommandLogDao().insertVoiceCommand(voiceCommand)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging notification interaction", e)
        }
    }
}