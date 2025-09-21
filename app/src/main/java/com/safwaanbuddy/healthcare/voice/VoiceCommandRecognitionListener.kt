// Voice Command Recognition Listener
// Handles speech recognition events for healthcare commands

package com.safwaanbuddy.healthcare.voice

import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safwaanbuddy.healthcare.MainActivity
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.util.*

class VoiceCommandRecognitionListener(
    private val context: Context,
    private val voiceCallback: VoiceCallback
) : RecognitionListener {
    
    companion object {
        private const val TAG = "VoiceCommandRecognitionListener"
    }
    
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
        voiceCallback.onReadyForSpeech(params)
    }
    
    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
        voiceCallback.onSpeechStarted()
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed - can be used for visual feedback
        // Log.d(TAG, "RMS changed: $rmsdB")
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
        // Log.d(TAG, "Buffer received: ${buffer?.size} bytes")
    }
    
    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
        voiceCallback.onSpeechEnded()
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
        voiceCallback.onError(errorMessage)
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        
        if (!matches.isNullOrEmpty()) {
            val command = matches[0]
            val confidence = confidences?.get(0) ?: 0.0f
            
            Log.d(TAG, "Voice command received: $command (confidence: $confidence)")
            
            // Process the command
            voiceCallback.onCommandProcessed(command, "Processing...", confidence)
            
            // Show a notification
            NotificationHelper.showVoiceCommandReceived(command)
            
            // Speak the response
            NotificationHelper.speakHealthcareResponse("I heard you say: $command. Let me process that for you.")
        } 
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            voiceCallback.onPartialResult(matches[0])
            
            // Show a notification with partial result
            NotificationHelper.showVoiceCommandPartialResult(matches[0])
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {
        // Additional events
        Log.d(TAG, "Event: $eventType")
    }
}

// Extension function to create a healthcare recognition listener
fun SpeechRecognizer.setHealthcareRecognitionListener(voiceCallback: VoiceCallback, context: Context) {
    this.setRecognitionListener(VoiceCommandRecognitionListener(context, voiceCallback))
}

// Composable function to remember the recognition listener
@Composable
fun rememberVoiceRecognitionListener(voiceCallback: VoiceCallback, context: Context): RecognitionListener {
    return remember(context, voiceCallback) {
        VoiceCommandRecognitionListener(context, voiceCallback)
    }
}

// ViewModel extension for voice command processing
fun ViewModel.processVoiceCommand(command: String, patientId: Long) {
    viewModelScope.launch {
        try {
            // Process the command
            // This would use the repository to process the command
            // and get a response
            
            // For now, just return a simple response
            val response = "I heard you say: $command"
            
            // Speak the response
            NotificationHelper.speakHealthcareResponse(response)
            
            // Show a notification
            NotificationHelper.showVoiceCommandProcessed(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            NotificationHelper.speakHealthcareResponse("I'm having trouble understanding. Could you rephrase your question?")
        }
    }
}