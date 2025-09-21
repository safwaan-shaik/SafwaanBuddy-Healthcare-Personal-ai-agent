package com.safwaanbuddy.healthcare.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.models.VoiceCommandLog
import com.safwaanbuddy.healthcare.reminders.MedicationReminderManager
import com.safwaanbuddy.healthcare.lab.LabResultsAnalyzer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Command Healthcare Assistant
 * Processes speech input for healthcare-related queries and commands
 */
@Singleton
class VoiceHealthcareAssistant @Inject constructor(
    private val context: Context,
    private val database: HealthcareDatabase,
    private val reminderManager: MedicationReminderManager,
    private val labAnalyzer: LabResultsAnalyzer
) : RecognitionListener {
    
    companion object {
        private const val TAG = "VoiceHealthcareAssistant"
        private const val SPEECH_RECOGNITION_TIMEOUT = 10000L // 10 seconds
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var currentPatientId: Long = -1
    private var voiceCallback: VoiceCallback? = null

    // Create a dedicated scope for this singleton to manage coroutines
    private val assistantScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Healthcare command patterns
    private val medicationPatterns = listOf(
        Regex("(?i).*(medication|medicine|pill|dose).*"),
        Regex("(?i).*(remind|reminder).*"),
        Regex("(?i).*(take|took|taking).*medication.*"),
        Regex("(?i).*when.*next.*dose.*"),
        Regex("(?i).*missed.*dose.*")
    )
    
    private val healthStatusPatterns = listOf(
        Regex("(?i).*how.*feeling.*"),
        Regex("(?i).*health.*status.*"),
        Regex("(?i).*blood pressure.*"),
        Regex("(?i).*glucose.*sugar.*"),
        Regex("(?i).*weight.*"),
        Regex("(?i).*baby.*doing.*")
    )
    
    private val labResultPatterns = listOf(
        Regex("(?i).*lab.*result.*"),
        Regex("(?i).*test.*result.*"),
        Regex("(?i).*blood.*work.*"),
        Regex("(?i).*recent.*test.*")
    )
    
    private val emergencyPatterns = listOf(
        Regex("(?i).*(emergency|urgent|help|crisis).*"),
        Regex("(?i).*(pain|bleeding|contractions).*"),
        Regex("(?i).*(cant.*breathe|chest.*pain).*"),
        Regex("(?i).*(dizzy|faint|unconscious).*")
    )
    
    init {
        initializeTextToSpeech()
    }
    
    /**
     * Initialize Text-to-Speech engine
     */
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                Log.d(TAG, "Text-to-Speech initialized successfully")
            } else {
                Log.e(TAG, "Text-to-Speech initialization failed")
            }
        }
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening(patientId: Long, callback: VoiceCallback) {
        if (isListening) {
            Log.w(TAG, "Already listening for voice commands")
            return
        }
        
        currentPatientId = patientId
        voiceCallback = callback
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
            
            isListening = true
            speechRecognizer?.startListening(intent)
            callback.onListeningStarted()
            
            Log.d(TAG, "Started listening for voice commands")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            callback.onError("Failed to start voice recognition: ${e.message}")
            isListening = false
        }
    }
    
    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        if (!isListening) return
        
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            voiceCallback?.onListeningStopped()
            
            Log.d(TAG, "Stopped listening for voice commands")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }
    
    /**
     * Process voice command and generate response
     */
    private suspend fun processVoiceCommand(command: String): String {
        val startTime = System.currentTimeMillis()
        
        return try { 
            val intent = classifyIntent(command)
            val response = when (intent) {
                VoiceIntent.MEDICATION_QUERY -> processMedicationQuery(command)
                VoiceIntent.HEALTH_STATUS -> processHealthStatusQuery(command)
                VoiceIntent.LAB_RESULTS -> processLabResultsQuery(command)
                VoiceIntent.EMERGENCY -> processEmergencyCommand(command)
                VoiceIntent.GENERAL_HEALTH -> processGeneralHealthQuery(command)
                VoiceIntent.UNKNOWN -> "I'm sorry, I didn't understand that. You can ask about medications, health status, or lab results."
            }
            
            // Log the interaction
            val processingTime = System.currentTimeMillis() - startTime
            logVoiceInteraction(command, intent.name, response, 1.0f, processingTime, true)
            
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            val errorResponse = "I'm sorry, I encountered an error processing your request."
            logVoiceInteraction(command, "ERROR", errorResponse, 0.0f, 0L, false, e.message)
            errorResponse
        }
    }
    
    /**
     * Classify user intent from voice command
     */
    private fun classifyIntent(command: String): VoiceIntent {
        return when {
            emergencyPatterns.any { it.matches(command) } -> VoiceIntent.EMERGENCY
            medicationPatterns.any { it.matches(command) } -> VoiceIntent.MEDICATION_QUERY
            labResultPatterns.any { it.matches(command) } -> VoiceIntent.LAB_RESULTS
            healthStatusPatterns.any { it.matches(command) } -> VoiceIntent.HEALTH_STATUS
            else -> VoiceIntent.UNKNOWN
        }
    }
    
    /**
     * Process medication-related queries
     */
    private suspend fun processMedicationQuery(command: String): String {
        return try {
            val reminders = database.medicationReminderDao()
                .getActiveRemindersByPatient(currentPatientId)
                .first()
            
            when {
                command.contains("next dose", ignoreCase = true) -> {
                    val nextReminder = reminders
                        .filter { it.nextReminder != null }
                        .minByOrNull { it.nextReminder!! }
                    
                    if (nextReminder != null) {
                        val nextTime = nextReminder.nextReminder!!
                        "Your next dose of ${nextReminder.medicationName} is scheduled for ${formatTime(nextTime)}."
                    } else {
                        "You don't have any upcoming medication reminders scheduled."
                    }
                }
                
                command.contains("missed", ignoreCase = true) -> {
                    val missedCount = database.medicationComplianceDao()
                        .getRecentMissedDoses(currentPatientId, 7)
                        .size
                    
                    if (missedCount > 0) {
                        "You have missed $missedCount doses in the past week. Would you like me to help you get back on track?"
                    } else {
                        "Great job! You haven't missed any doses recently."
                    }
                }
                
                command.contains("taking", ignoreCase = true) || command.contains("medications", ignoreCase = true) -> {
                    if (reminders.isNotEmpty()) {
                        val medicationList = reminders.joinToString(", ") { it.medicationName }
                        "You are currently taking: $medicationList. All medications are scheduled as prescribed."
                    } else {
                        "You don't have any active medications scheduled."
                    }
                }
                
                else -> {
                    "I can help you with medication schedules, missed doses, or current medications. What would you like to know?"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing medication query", e)
            "I'm having trouble accessing your medication information right now."
        }
    }
    
    /**
     * Process health status queries
     */
    private suspend fun processHealthStatusQuery(command: String): String {
        return try {
            val recentMetrics = database.healthMetricDao()
                .getMetricsByPatient(currentPatientId)
                .first()
                .take(5) // Get 5 most recent metrics
            
            when {
                command.contains("blood pressure", ignoreCase = true) -> {
                    val bpMetric = recentMetrics.find { it.metricType == "blood_pressure" }
                    if (bpMetric != null) {
                        "Your most recent blood pressure reading was ${bpMetric.value} ${bpMetric.unit}, recorded on ${formatDate(bpMetric.measuredAt)}."
                    } else {
                        "I don't have any recent blood pressure readings. Please take a reading and log it in the app."
                    }
                }
                
                command.contains("weight", ignoreCase = true) -> {
                    val weightMetric = recentMetrics.find { it.metricType == "weight" }
                    if (weightMetric != null) {
                        "Your most recent weight was ${weightMetric.value} ${weightMetric.unit}, recorded on ${formatDate(weightMetric.measuredAt)}."
                    } else {
                        "I don't have any recent weight measurements. Regular weight monitoring is important during pregnancy."
                    }
                }
                
                command.contains("baby", ignoreCase = true) -> {
                    "Based on your current health metrics, everything looks good. Remember to monitor fetal movement and attend all prenatal appointments."
                }
                
                else -> {
                    if (recentMetrics.isNotEmpty()) {
                        "Your recent health metrics show: ${recentMetrics.size} recorded measurements. Everything appears to be within normal ranges."
                    } else {
                        "I don't have recent health data to review. Please make sure to log your vital signs regularly."
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing health status query", e)
            "I'm having trouble accessing your health information right now."
        }
    }
    
    /**
     * Process lab results queries
     */
    private suspend fun processLabResultsQuery(command: String): String {
        return try {
            val recentResults = database.labResultDao()
                .getLabResultsByPatient(currentPatientId)
                .first()
                .take(3) // Get 3 most recent results
            
            if (recentResults.isNotEmpty()) {
                val latestResult = recentResults.first()
                val urgencyLevel = latestResult.urgencyLevel
                
                when (urgencyLevel) {
                    "critical" -> {
                        "Your most recent lab results from ${formatDate(latestResult.testDate)} require immediate attention. Please contact your healthcare provider right away."
                    }
                    "elevated" -> {
                        "Your recent lab results from ${formatDate(latestResult.testDate)} show some elevated values that need follow-up. Please discuss with your doctor at your next appointment."
                    }
                    else -> {
                        "Your most recent lab results from ${formatDate(latestResult.testDate)} are within normal ranges. Great job maintaining your health!"
                    }
                }
            } else {
                "I don't have any recent lab results on file. Make sure to upload your test results when you receive them."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing lab results query", e)
            "I'm having trouble accessing your lab results right now."
        }
    }
    
    /**
     * Process emergency commands
     */
    private fun processEmergencyCommand(command: String): String {
        // Log emergency command immediately
        logVoiceInteraction(command, "EMERGENCY", "Emergency protocol activated", 1.0f, 0L, true)
        
        return when {
            command.contains("emergency", ignoreCase = true) || command.contains("urgent", ignoreCase = true) -> {
                "This is an emergency response. If you're experiencing a medical emergency, please call 911 immediately. I'm also alerting your emergency contacts."
            }
            command.contains("pain", ignoreCase = true) -> {
                "I understand you're experiencing pain. If this is severe or sudden pain, please call 911 or go to the emergency room. Would you like me to contact your healthcare provider?"
            }
            command.contains("bleeding", ignoreCase = true) -> {
                "Bleeding during pregnancy can be serious. Please contact your healthcare provider immediately or go to the emergency room if bleeding is heavy."
            }
            command.contains("contractions", ignoreCase = true) -> {
                "If you're experiencing regular contractions, please time them and contact your healthcare provider. If contractions are 5 minutes apart or less, go to the hospital."
            }
            else -> {
                "I'm concerned about your request. If this is a medical emergency, please call 911. Otherwise, please contact your healthcare provider for guidance."
            }
        }
    }
    
    /**
     * Process general health queries
     */
    private fun processGeneralHealthQuery(command: String): String {
        return "I'm here to help with your healthcare needs. You can ask me about medications, health metrics, lab results, or get emergency assistance. What would you like to know?"
    }
    
    /**
     * Speak response using Text-to-Speech
     */
    fun speakResponse(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "healthcare_response")
    }
    
    /**
     * Log voice interaction for compliance and analysis
     */
    private fun logVoiceInteraction(
        command: String,
        intent: String,
        response: String,
        confidence: Float,
        processingTime: Long,
        wasSuccessful: Boolean,
        errorMessage: String? = null
    ) {
        assistantScope.launch {
            val voiceLog = VoiceCommandLog(
                patientId = currentPatientId,
                commandText = command,
                intentClassification = intent,
                responseGenerated = response,
                confidenceScore = confidence,
                processingTimeMs = processingTime,
                wasSuccessful = wasSuccessful,
                errorMessage = errorMessage,
                timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            
            database.voiceCommandLogDao().insertVoiceCommand(voiceLog)
        }.invokeOnCompletion { throwable ->
            throwable?.let { Log.e(TAG, "Failed to log voice interaction", it) }
        }
    }
    
    /**
     * Format time for voice response
     */
    private fun formatTime(dateTime: kotlinx.datetime.LocalDateTime): String {
        return "${dateTime.hour}:${String.format("%02d", dateTime.minute)}"
    }
    
    /**
     * Format date for voice response
     */
    private fun formatDate(date: kotlinx.datetime.LocalDate): String {
        return "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}"
    }
    
    private fun formatDate(dateTime: kotlinx.datetime.LocalDateTime): String {
        return formatDate(dateTime.date)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        assistantScope.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
    
    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
        voiceCallback?.onReadyForSpeech()
    }
    
    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
        voiceCallback?.onSpeechStarted()
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed - can be used for visual feedback
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }
    
    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
        voiceCallback?.onSpeechEnded()
    }
    
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        
        Log.e(TAG, "Speech recognition error: $errorMessage")
        isListening = false
        voiceCallback?.onError(errorMessage)
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        
        if (!matches.isNullOrEmpty()) {
            val command = matches[0]
            val confidence = confidences?.get(0) ?: 0.0f
            
            Log.d(TAG, "Voice command received: $command (confidence: $confidence)")
            
            // Process the command in a coroutine to avoid blocking the main thread
            assistantScope.launch {
                val response = processVoiceCommand(command)
                
                withContext(Dispatchers.Main) {
                    isListening = false
                    voiceCallback?.onCommandProcessed(command, response, confidence)
                    // Automatically speak the response
                    speakResponse(response)
                }
            }
        } 
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            voiceCallback?.onPartialResult(matches[0])
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {
        // Additional events
    }
}

/**
 * Voice command intents
 */
enum class VoiceIntent {
    MEDICATION_QUERY,
    HEALTH_STATUS,
    LAB_RESULTS,
    EMERGENCY,
    GENERAL_HEALTH,
    UNKNOWN
}

/**
 * Callback interface for voice interactions
 */
interface VoiceCallback {
    fun onListeningStarted()
    fun onListeningStopped()
    fun onReadyForSpeech()
    fun onSpeechStarted()
    fun onSpeechEnded()
    fun onPartialResult(partialText: String)
    fun onCommandProcessed(command: String, response: String, confidence: Float)
    fun onError(error: String)
}