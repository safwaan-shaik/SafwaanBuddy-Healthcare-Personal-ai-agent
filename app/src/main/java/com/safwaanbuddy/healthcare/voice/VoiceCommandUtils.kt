// Voice Command Utilities
// Helper functions for voice command processing

package com.safwaanbuddy.healthcare.voice

import android.speech.SpeechRecognizer
import androidx.compose.runtime.*
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*
import java.util.*

/**
 * Create a voice command intent with healthcare-specific settings
 * @return The configured SpeechRecognizer intent
 */
fun createHealthcareIntent(): SpeechRecognizer.Intent {
    val intent = SpeechRecognizer.Intent(SpeechRecognizer.ACTION_RECOGNIZE_SPEECH)
    intent.putExtra(SpeechRecognizer.EXTRA_LANGUAGE_MODEL, SpeechRecognizer.LANGUAGE_MODEL_FREE_FORM)
    intent.putExtra(SpeechRecognizer.EXTRA_LANGUAGE, "en-US")
    intent.putExtra(SpeechRecognizer.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L) // 3 seconds of silence to end speech
    intent.putExtra(SpeechRecognizer.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L) // Minimum 1 second of speech
    intent.putExtra(SpeechRecognizer.EXTRA_MAX_RESULTS, 1) // Only keep the most confident result
    
    return intent
}

/**
 * Create a voice command intent with healthcare-specific settings for continuous listening
 * @return The configured SpeechRecognizer intent
 */
fun createContinuousHealthcareIntent(): SpeechRecognizer.Intent {
    val intent = createHealthcareIntent()
    intent.putExtra(SpeechRecognizer.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L) // 10 seconds of silence to end speech
    intent.putExtra(SpeechRecognizer.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L) // Minimum 2 seconds of speech
    
    return intent
}

/**
 * Format a confidence score as a percentage
 * @param confidence The confidence score (0.0-1.0)
 * @return The formatted percentage
 */
fun formatConfidencePercentage(confidence: Float): String {
    return "${(confidence * 100).toInt()}%"
}

/**
 * Get a confidence level description
 * @param confidence The confidence score (0.0-1.0)
 * @return A description of the confidence level
 */
fun getConfidenceLevelDescription(confidence: Float): String {
    return when {
        confidence >= 0.8f -> "High confidence"
        confidence >= 0.6f -> "Medium confidence"
        confidence >= 0.4f -> "Low confidence"
        else -> "Uncertain"
    }
}

/**
 * Show a voice command response
 * @param response The response to show
 */
@Composable
fun showVoiceCommandResponse(response: String) {
    DisposableEffect(Unit) {
        onDispose {
            NotificationHelper.speakHealthcareResponse(response)
        }
    }
    
    // Show the response in a dialog
    AlertDialog(
        onDismissRequest = { /* Do nothing on dismiss */ },
        title = { Text("SafwaanBuddy") },
        text = { Text(response) },
        confirmButton = {
            TextButton(onClick = { /* Handle confirmation */ }) {
                Text("OK")
            }
        }
    )
}