// Voice Command Data Access Object
// Provides database access for voice command logs

package com.safwaanbuddy.healthcare.voice

import androidx.room.*
import kotlinx.coroutines.*
import java.util.*

/**
 * Data Access Object for voice command logs
 * @Dao annotation allows Room to know this is a DAO
 */
@Dao
interface VoiceCommandDao {
    
    /**
     * Insert a voice command log into the database
     * @param voiceCommand The voice command to insert
     * @return The ID of the inserted voice command
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceCommand(voiceCommand: VoiceCommandLog): Long
    
    /**
     * Get all voice commands for a patient
     * @param patientId The ID of the patient
     * @return List of voice commands for the patient
     * @Query annotation provides the SQL query to get voice commands by patient ID
     */
    @Query("SELECT * FROM voice_commands WHERE patient_id = :patientId")
    fun getVoiceCommandsByPatient(patientId: Long): List<CoroutineScope>
    
    /**
     * Get voice commands for a patient in a date range
     * @param patientId The ID of the patient
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     * @return List of voice commands in the date range
     * @Query annotation provides the SQL query to get voice commands by patient and date range
     */
    @Query("SELECT * FROM voice_commands WHERE patient_id = :patientId AND timestamp BETWEEN :startDate AND :endDate")
    fun getVoiceCommandsByDateRange(
        patientId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<CoroutineScope>
    
    /**
     * Get voice commands by intent classification
     * @param patientId The ID of the patient
     * @param intent The intent classification to search for
     * @return List of voice commands with the specified intent
     * @Query annotation provides the SQL query to get voice commands by intent
     */
    @Query("SELECT * FROM voice_commands WHERE patient_id = :patientId AND intent_classification = :intent")
    fun getVoiceCommandsByIntent(patientId: Long, intent: String): List<CoroutineScope>
    
    /**
     * Get recent voice commands for a patient
     * @param patientId The ID of the patient
     * @param limit The maximum number of commands to return
     * @return List of recent voice commands
     * @Query annotation provides the SQL query to get recent voice commands
     */
    @Query("SELECT * FROM voice_commands WHERE patient_id = :patientId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentVoiceCommands(patientId: Long, limit: Int): List<CoroutineScope>
    
    /**
     * Get voice commands by confidence score
     * @param patientId The ID of the patient
     * @param minConfidence The minimum confidence score to search for
     * @return List of voice commands with confidence above the threshold
     * @Query annotation provides the SQL query to get voice commands by confidence
     */
    @Query("SELECT * FROM voice_commands WHERE patient_id = :patientId AND confidence_score >= :minConfidence")
    fun getVoiceCommandsByConfidence(patientId: Long, minConfidence: Float): List<CoroutineScope>
    
    /**
     * Delete all voice commands for a patient
     * @param patientId The ID of the patient
     * @Query annotation provides the SQL query to delete voice commands by patient
     */
    @Query("DELETE FROM voice_commands WHERE patient_id = :patientId")
    fun deleteAllVoiceCommands(patientId: Long)
    
    /**
     * Delete a specific voice command
     * @param voiceCommand The voice command to delete
     * @Query annotation provides the SQL query to delete a voice command
     */
    @Delete
    fun deleteVoiceCommand(voiceCommand: VoiceCommandLog)
    
    companion object {
        // Confidence thresholds for queries
        const val CONFIDENCE_THRESHOLD_HIGH = 0.8f
        const val CONFIDENCE_THRESHOLD_MEDIUM = 0.6f
    }
}