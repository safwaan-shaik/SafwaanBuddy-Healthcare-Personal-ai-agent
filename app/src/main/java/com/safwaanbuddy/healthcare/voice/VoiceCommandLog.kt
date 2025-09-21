// Voice Command Log
// Tracks voice interactions for compliance and analysis

package com.safwaanbuddy.healthcare.voice

import androidx.room.*
import kotlinx.coroutines.*
import java.util.*

/**
 * Entity class to track voice commands and responses
 * @Entity annotation creates a table in the database
 */
@Entity(tableName = "voice_commands")
@TypeConverters(DateConverters::class)
@Parcelize
@OptIn(ExperimentalTypeInference::class)
@TypeInference(
    inference = "voiceCommandLog",
    type = "voiceCommandLog",
    description = "Voice command logs for healthcare app"
)
data class VoiceCommandLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "command_text")
    val commandText: String,
    
    @ColumnInfo(name = "intent_classification")
    val intentClassification: String,
    
    @ColumnInfo(name = "response_generated")
    val responseGenerated: String,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long,
    
    @ColumnInfo(name = "was_successful")
    val wasSuccessful: Boolean,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
) : Parcelable {
    
    companion object {
        // Default confidence for new commands
        private const val DEFAULT_CONFIDENCE = 0.0f
        
        // Default command and response
        private const val DEFAULT_COMMAND = ""
        private const val DEFAULT_RESPONSE = ""
        
        // Default processing time
        private const val DEFAULT_PROCESSING_TIME = 0L
        
        // Default success status
        private val DEFAULT_SUCCESS = false
    }
    
    /**
     * Create a new VoiceCommandLog from a command and response
     * @param command The user's voice command
     * @param response The generated response
     * @param confidence The confidence score for the command classification
     * @param wasSuccessful Whether the command was processed successfully
     * @return A new VoiceCommandLog instance
     */
    fun create(
        command: String,
        response: String,
        confidence: Float,
        wasSuccessful: Boolean
    ): VoiceCommandLog {
        return VoiceCommandLog(
            commandText = command,
            intentClassification = classifyIntent(command),
            responseGenerated = response,
            confidenceScore = confidence,
            processingTimeMs = 0L, // Will be calculated during processing
            wasSuccessful = wasSuccessful
        )
    }
    
    /**
     * Classify the intent of a voice command
     * @param command The voice command text
     * @return The intent classification
     */
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
    
    /**
     * Get a sample voice command for testing
     * @return A sample VoiceCommandLog instance
     */
    fun getSample(): VoiceCommandLog {
        return VoiceCommandLog(
            commandText = "How are my medications doing?",
            intentClassification = "medication",
            responseGenerated = "You have 3 medications active: Prenatal vitamins, Iron supplements, and Folic acid. All are on schedule.",
            confidenceScore = 0.95f,
            processingTimeMs = 1200L,
            wasSuccessful = true,
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
    
    /**
     * Get a sample error voice command for testing
     * @return A sample VoiceCommandLog instance with error
     */
    fun getErrorSample(): VoiceCommandLog {
        return VoiceCommandLog(
            commandText = "What's my medication status?",
            intentClassification = "medication",
            responseGenerated = "",
            confidenceScore = 0.6f,
            processingTimeMs = 1500L,
            wasSuccessful = false,
            errorMessage = "Database connection failed",
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
    
    /**
     * Convert this VoiceCommandLog to a string representation
     * @return A string with the command and response
     */
    override fun toString(): String {
        return "VoiceCommandLog(id=$id, command='$commandText', intent='$intentClassification', response='$responseGenerated', confidence=$confidenceScore, time=$processingTimeMs ms, success=$wasSuccessful, error=$errorMessage, timestamp=$timestamp)"
    }
    
    /**
     * Get a summary of this voice command
     * @return A summary string
     */
    fun getSummary(): String {
        return "Command: $commandText\nIntent: $intentClassification\nConfidence: ${String.format("%.1f", confidenceScore * 100)}%\nSuccess: $wasSuccessful\nTimestamp: ${formatDate(timestamp)}"
    }
    
    companion object {
        // Confidence thresholds
        const val CONFIDENCE_THRESHOLD_HIGH = 0.8f
        const val CONFIDENCE_THRESHOLD_MEDIUM = 0.6f
