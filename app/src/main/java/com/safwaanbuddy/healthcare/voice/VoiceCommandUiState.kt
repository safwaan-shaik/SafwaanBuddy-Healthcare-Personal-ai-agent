// Voice Command UI State
// Holds the state for the voice command screen

package com.safwaanbuddy.healthcare.voice

import androidx.compose.runtime.*
import kotlinx.datetime.*
import java.util.*

/**
 * UI state for voice command screen
 * @param isProcessing Whether a command is currently being processed
 * @param isListening Whether the app is listening for voice input
 * @param command The current voice command
 * @param intent The classified intent of the command
 * @param response The generated response
 * @param confidence The confidence score for the command classification
 * @param wasSuccessful Whether the command was processed successfully
 * @param timestamp The time the command was processed
 */
data class VoiceCommandUiState(
    val isProcessing: Boolean = false,
    val isListening: Boolean = false,
    val command: String = "",
    val intent: String = "",
    val response: String = "",
    val confidence: Float = 0.0f,
    val wasSuccessful: Boolean = false,
    val timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
) {
    
    /**
     * Get a summary of the current state
     * @return A summary string
     */
    fun getSummary(): String {
        return "Command: $command\nIntent: $intent\nConfidence: ${String.format("%.1f", confidence * 100)}%\nSuccess: $wasSuccessful\nTimestamp: $timestamp"
    }
    
    /**
     * Get a confidence level description
     * @return A confidence level description
     */
    fun getConfidenceDescription(): String {
        return when {
            confidence >= 0.8f -> "High confidence"
            confidence >= 0.6f -> "Medium confidence"
            confidence >= 0.4f -> "Low confidence"
            else -> "Uncertain"
        }
    }
    
    companion object {
        // Default state
        fun defaultState() = VoiceCommandUiState()
        
        // Empty state
        fun empty() = VoiceCommandUiState(
            isProcessing = false,
            isListening = false,
            command = "",
            intent = "",
            response = "",
            confidence = 0.0f,
            wasSuccessful = false,
            timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
    }
}