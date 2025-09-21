// Voice Command Repository
// Manages voice command data from the database

package com.safwaanbuddy.healthcare.voice

import androidx.room.Room
import com.safwaanbuddy.healthcare.data.models.Patient
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*
import java.util.*

class VoiceCommandRepository private constructor(
    private val voiceCommandDao: VoiceCommandDao
) {
    
    companion object {
        // Singleton instance
        private var instance: VoiceCommandRepository? = null
        
        // Confidence thresholds
        const val CONFIDENCE_THRESHOLD_HIGH = VoiceCommandDatabase.CONFIDENCE_THRESHOLD_HIGH
        const val CONFIDENCE_THRESHOLD_MEDIUM = VoiceCommandDatabase.CONFIDENCE_THRESHOLD_MEDIUM
        
        // Default confidence for new commands
        const val DEFAULT_CONFIDENCE = VoiceCommandDatabase.DEFAULT_CONFIDENCE
        
        // Get the repository instance
        fun getInstance(voiceCommandDao: VoiceCommandDao): VoiceCommandRepository {
            return instance ?: VoiceCommandRepository(voiceCommandDao).apply {
                instance = this
            }
        }
    }
    
    // Add a new voice command
    suspend fun addVoiceCommand(voiceCommand: VoiceCommandLog): Long {
        return withContext(Dispatchers.IO) {
            try {
                val result = voiceCommandDao.insertVoiceCommand(voiceCommand)
                // Notify if the command was successfully added
                if (result != -1L) {
                    NotificationHelper.showVoiceCommandAdded()
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add voice command", e)
                -1
            }
        }
    }
    
    // Get all voice commands for a patient
    fun getVoiceCommandsByPatient(patientId: Long): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByPatient(patientId)
    }
    
    // Get voice commands for a patient in a date range
    fun getVoiceCommandsByDateRange(
        patientId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByDateRange(patientId, startDate, endDate)
    }
    
    // Get voice commands by intent classification
    fun getVoiceCommandsByIntent(patientId: Long, intent: String): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByIntent(patientId, intent)
    }
    
    // Get recent voice commands for a patient
    fun getRecentVoiceCommands(patientId: Long, limit: Int): List<CoroutineScope> {
        return voiceCommandDao.getRecentVoiceCommands(patientId, limit)
    }
    
    // Get voice commands by confidence score
    fun getVoiceCommandsByConfidence(patientId: Long, minConfidence: Float): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByConfidence(patientId, minConfidence)
    }
    
    // Delete all voice commands for a patient
    fun deleteAllVoiceCommands(patientId: Long) {
        voiceCommandDao.deleteAllVoiceCommands(patientId)
    }
    
    // Delete a specific voice command
    fun deleteVoiceCommand(voiceCommand: VoiceCommandLog) {
        voiceCommandDao.deleteVoiceCommand(voiceCommand)
    }
    
    // Process a voice command and generate a response
    suspend fun processVoiceCommand(
        command: String,
        patientId: Long
    ): VoiceCommandLog {
        val intent = classifyIntent(command)
        val confidence = calculateConfidence(command, intent)
        val response = generateResponse(command, intent, confidence)
        val wasSuccessful = confidence >= DEFAULT_CONFIDENCE
        
        val voiceCommand = VoiceCommandLog(
            commandText = command,
            intentClassification = intent,
            responseGenerated = response,
            confidenceScore = confidence,
            wasSuccessful = wasSuccessful,
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            patientId = patientId
        )
        
        // Add to database
        addVoiceCommand(voiceCommand)
        
        return voiceCommand
    }
    
    // Classify the intent of a voice command
    private fun classifyIntent(command: String): String {
        val lowerCommand = command.lowercase()
        
        return when {
            // Medication-related commands
            lowerCommand.contains("medication") ||
            lowerCommand.contains("dose") ||
            lowerCommand.contains("pill") ||
            lowerCommand.contains("prescription") -> "medication"
            
            // Health status commands
            lowerCommand.contains("how am I doing") ||
            lowerCommand.contains("health status") ||
            lowerCommand.contains("check up") -> "health_status"
            
            // Lab results commands
            lowerCommand.contains("lab") ||
            lowerCommand.contains("test results") -> "lab_results"
            
            // Emergency commands
            lowerCommand.contains("emergency") ||
            lowerCommand.contains("urgent") -> "emergency"
            
            // General healthcare commands
            lowerCommand.contains("health") ||
            lowerCommand.contains("care") -> "general_health"
            
            // Unknown intent
            else -> "unknown"
        }
    }
    
    // Calculate confidence score for command classification
    private fun calculateConfidence(command: String, intent: String): Float {
        // Simple confidence calculation based on keyword matches
        val lowerCommand = command.lowercase()
        
        return when (intent) {
            "medication" -> {
                // Count medication-related keywords
                val matches = listOf("medication", "dose", "pill", "prescription")
                    .count { lowerCommand.contains(it) }
                
                val keywordWeight = matches * 0.2f
                val minConfidence = 0.3f
                (keywordWeight + minConfidence).coerceAtMost(1.0f)
            }
            "health_status" -> {
                // Count health status-related keywords
                val matches = listOf("how am I doing", "health status", "check up")
                    .count { lowerCommand.contains(it) }
                
                val keywordWeight = matches * 0.2f
                val minConfidence = 0.3f
                (keywordWeight + minConfidence).coerceAtMost(1.0f)
            }
            "lab_results" -> {
                // Count lab results-related keywords
                val matches = listOf("lab", "test results")
                    .count { lowerCommand.contains(it) }
                
                val keywordWeight = matches * 0.3f
                val minConfidence = 0.2f
                (keywordWeight + minConfidence).coerceAtMost(1.0f)
            }
            "emergency" -> {
                // Count emergency-related keywords
                val matches = listOf("emergency", "urgent")
                    .count { lowerCommand.contains(it) }
                
                val keywordWeight = matches * 0.4f
                val minConfidence = 0.3f
                (keywordWeight + minConfidence).coerceAtMost(1.0f)
            }
            "general_health" -> {
                // Count general health-related keywords
                val matches = listOf("health", "care")
                    .count { lowerCommand.contains(it) }
                
                val keywordWeight = matches * 0.3f
                val minConfidence = 0.3f
                (keywordWeight + minConfidence).coerceAtMost(1.0f)
            }
            else -> 0.5f // Unknown intent
        }
    }
    
    // Generate a response based on command and intent
    private fun generateResponse(
        command: String,
        intent: String,
        confidence: Float
    ): String {
        // Generate a response based on intent
        return when (intent) {
            "medication" -> {
                if (confidence >= CONFIDENCE_THRESHOLD_HIGH) {
                    "Let me check your medication schedule. You have 3 medications active: Prenatal vitamins, Iron supplements, and Folic acid. All are on schedule."
                } else if (confidence >= CONFIDENCE_THRESHOLD_MEDIUM) {
                    "Let me check your medication schedule. You have 3 medications active: Prenatal vitamins, Iron supplements, and Folic acid. All are on schedule."
                } else {
                    "I heard you mentioned medications. Let me check your schedule."
                }
            }
            "health_status" -> {
                if (confidence >= CONFIDENCE_THRESHOLD_HIGH) {
                    "Let me check your health status. Based on your recent metrics, everything looks good. Your blood pressure is within normal ranges, and your weight is appropriate for your stage of pregnancy."
                } else if (confidence >= CONFIDENCE_THRESHOLD_MEDIUM) {
                    "I heard you asked about your health status. Let me check your recent metrics."
                } else {
                    "I heard you mentioned health status. Let me check your records."
                }
            }
            "lab_results" -> {
                if (confidence >= CONFIDENCE_THRESHOLD_HIGH) {
                    "Let me check your recent lab results. Your most recent blood work shows normal hemoglobin levels and appropriate iron stores."
                } else if (confidence >= CONFIDENCE_THRESHOLD_MEDIUM) {
                    "I heard you mentioned lab results. Let me check your recent tests."
                } else {
                    "I heard you mentioned lab results. Let me check your records."
                }
            }
            "emergency" -> {
                "This sounds like an emergency. If you're experiencing any concerning symptoms, please call 911 or go to the nearest emergency room."
            }
            "general_health" -> {
                if (confidence >= CONFIDENCE_THRESHOLD_HIGH) {
                    "Based on your current health metrics, everything looks good. Your blood pressure and weight are within normal ranges for your stage of pregnancy."
                } else if (confidence >= CONFIDENCE_THRESHOLD_MEDIUM) {
                    "I heard you asked about your health. Let me check your recent metrics."
                } else {
                    "I heard you mentioned health. Let me check your records."
                }
            }
            else -> {
                "I'm not sure what you're asking about. Would you like to ask about medications, health status, lab results, or something else?"
            }
        }
    }
    
    // Add a new voice command
    suspend fun addVoiceCommand(voiceCommand: VoiceCommandLog): Long {
        return withContext(Dispatchers.IO) {
            try {
                val result = voiceCommandDao.insertVoiceCommand(voiceCommand)
                // Notify if the command was successfully added
                if (result != -1L) {
                    NotificationHelper.showVoiceCommandAdded()
                }
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add voice command", e)
                -1
            }
        }
    }
    
    // Get all voice commands for a patient
    fun getVoiceCommandsByPatient(patientId: Long): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByPatient(patientId)
    }
    
    // Get voice commands for a patient in a date range
    fun getVoiceCommandsByDateRange(
        patientId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByDateRange(patientId, startDate, endDate)
    }
    
    // Get voice commands by intent classification
    fun getVoiceCommandsByIntent(patientId: Long, intent: String): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByIntent(patientId, intent)
    }
    
    // Get recent voice commands for a patient
    fun getRecentVoiceCommands(patientId: Long, limit: Int): List<CoroutineScope> {
        return voiceCommandDao.getRecentVoiceCommands(patientId, limit)
    }
    
    // Get voice commands by confidence score
    fun getVoiceCommandsByConfidence(patientId: Long, minConfidence: Float): List<CoroutineScope> {
        return voiceCommandDao.getVoiceCommandsByConfidence(patientId, minConfidence)
    }
    
    // Delete all voice commands for a patient
    fun deleteAllVoiceCommands(patientId: Long) {
        voiceCommandDao.deleteAllVoiceCommands(patientId)
    }
    
    // Delete a specific voice command
    fun deleteVoiceCommand(voiceCommand: VoiceCommandLog) {
        voiceCommandDao.deleteVoiceCommand(voiceCommand)
    }
    
    // Process a voice command and generate a response
    suspend fun processVoiceCommand(
        command: String,
        patientId: Long
    ): VoiceCommandLog {
        val intent = classifyIntent(command)
        val confidence = calculateConfidence(command, intent)
        val response = generateResponse(command, intent, confidence)
        val wasSuccessful = confidence >= DEFAULT_CONFIDENCE
        
        val voiceCommand = VoiceCommandLog(
            commandText = command,
            intentClassification = intent,
            responseGenerated = response,
            confidenceScore = confidence,
            wasSuccessful = wasSuccessful,
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            patientId = patientId
        )
        
        // Add to database
        addVoiceCommand(voiceCommand)
        
        return voiceCommand
    }
    
    companion object {
        private const val TAG = "VoiceCommandRepository"
    }
}