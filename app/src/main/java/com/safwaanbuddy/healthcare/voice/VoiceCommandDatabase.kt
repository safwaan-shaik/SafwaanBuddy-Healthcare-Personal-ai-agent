// Voice Command Database
// Room database for voice command logs

package com.safwaanbuddy.healthcare.voice

import androidx.room.*
import com.safwaanbuddy.healthcare.data.models.Patient
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*
import java.util.*

/**
 * Abstract class for voice command database
 * @Database annotation defines entities and database version
 */
@Database(entities = [VoiceCommandLog::class], version = 2, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class VoiceCommandDatabase : RoomDatabase() {
    
    /**
     * Get the voice command data access object
     * @return The VoiceCommandDao
     */
    abstract fun voiceCommandDao(): VoiceCommandDao
    
    companion object {
        // Database name
        private const val DATABASE_NAME = "voice_commands.db"
        
        // Confidence thresholds
        const val CONFIDENCE_THRESHOLD_HIGH = 0.8f
        const val CONFIDENCE_THRESHOLD_MEDIUM = 0.6f
        
        // Default confidence for new commands
        const val DEFAULT_CONFIDENCE = 0.0f
    }
    
    /**
     * Create a sample voice command for testing
     * @param patient The patient to associate with the command
     * @return A sample VoiceCommandLog
     */
    fun createSampleVoiceCommand(patient: Patient): VoiceCommandLog {
        return VoiceCommandLog(
            commandText = "How are my medications doing?",
            intentClassification = "medication",
            responseGenerated = "You have 3 medications active: Prenatal vitamins, Iron supplements, and Folic acid. All are on schedule.",
            confidenceScore = 0.95f,
            wasSuccessful = true,
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            patientId = patient.id
        )
    }
    
    /**
     * Get a sample error voice command
     * @param patient The patient to associate with the command
     * @return A sample VoiceCommandLog with error
     */
    fun getErrorSample(patient: Patient): VoiceCommandLog {
        return VoiceCommandLog(
            commandText = "What's my medication status?",
            intentClassification = "medication",
            responseGenerated = "",
            confidenceScore = 0.6f,
            wasSuccessful = false,
            errorMessage = "Database connection failed",
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            patientId = patient.id
        )
    }
    
    /**
     * Get voice commands by confidence score
     * @param minConfidence The minimum confidence score to search for
     * @return List of voice commands with confidence above the threshold
     * @Query annotation provides the SQL query to get voice commands by confidence
     */
    fun getVoiceCommandsByConfidence(patientId: Long, minConfidence: Float): List<CoroutineScope>
    
    /**
     * Delete all voice commands for a patient
     * @param patientId The ID of the patient
     * @Query annotation provides the SQL query to delete voice commands by patient
     */
    fun deleteAllVoiceCommands(patientId: Long)
    
    companion object {
        // Confidence thresholds for queries
        const val CONFIDENCE_THRESHOLD_HIGH = 0.8f
        const val CONFIDENCE_THRESHOLD_MEDIUM = 0.6f
    }
}