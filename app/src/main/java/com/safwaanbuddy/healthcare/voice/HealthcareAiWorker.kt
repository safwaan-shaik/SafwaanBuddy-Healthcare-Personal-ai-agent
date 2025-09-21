// Healthcare AI Worker
// Background worker for proactive health check-ins

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
class HealthcareAiWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthcareAiService: HealthcareAiService,
    private val database: HealthcareDatabase
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "HealthcareAiWorker"
        const val WORK_NAME = "healthcare_ai_worker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Get the current patient
            val patientId = inputData.getLong("patient_id", -1)
            if (patientId == -1L) {
                return Result.failure()
            }
            
            // Generate a proactive check-in message
            val checkInMessage = generateProactiveCheckIn(patientId)
            
            // Show a notification with the check-in message
            NotificationHelper.showHealthcareCheckIn(applicationContext, checkInMessage)
            
            // Log the interaction
            logAiInteraction(patientId, "proactive_check_in", checkInMessage)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in Healthcare AI Worker", e)
            Result.failure()
        }
    }
    
    /**
     * Generate a proactive check-in message
     * @param patientId The ID of the current patient
     * @return The check-in message
     */
    private suspend fun generateProactiveCheckIn(patientId: Long): String {
        return try {
            // Get patient information
            val patient = database.patientDao().getPatientById(patientId)
            
            // Generate a personalized check-in message
            val checkInMessage = if (patient != null) {
                "Hey ${patient.name}, how are you feeling today? Remember to take your prenatal vitamins!"
            } else {
                "Hey there! How are you feeling today? Remember to take your medications!"
            }
            
            // Use AI to generate a more personalized message
            try {
                val aiMessage = healthcareAiService.generateProactiveCheckIn(patientId).get()
                if (aiMessage.isNotEmpty()) {
                    aiMessage
                } else {
                    checkInMessage
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI check-in message", e)
                checkInMessage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating check-in message", e)
            "Hey there! How are you feeling today?"
        }
    }
    
    /**
     * Log an AI interaction
     * @param patientId The ID of the patient
     * @param interactionType The type of interaction
     * @param message The message content
     */
    private suspend fun logAiInteraction(patientId: Long, interactionType: String, message: String) {
        try {
            val voiceCommand = VoiceCommandLog(
                commandText = "AI_$interactionType",
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
            Log.e(TAG, "Error logging AI interaction", e)
        }
    }
}