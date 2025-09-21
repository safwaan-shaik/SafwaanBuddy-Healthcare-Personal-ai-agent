// Voice Command View Model
// ViewModel for voice command screen

package com.safwaanbuddy.healthcare.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safwaanbuddy.healthcare.data.models.Patient
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*
import java.util.*

/**
 * ViewModel for voice command screen
 * @HiltViewModel annotation allows Hilt to inject dependencies
 */
@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val voiceCommandRepository: VoiceCommandRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VoiceCommandUiState())
    val uiState: StateFlow<VoiceCommandUiState> = _uiState.asStateFlow()
    
    /**
     * Process a voice command and update UI state
     * @param command The voice command to process
     * @param patient The current patient
     */
    fun processVoiceCommand(command: String, patient: Patient) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }
                
                // Process the command
                val voiceCommand = voiceCommandRepository.processVoiceCommand(command, patient.id)
                
                // Update UI with results
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        command = voiceCommand.commandText,
                        intent = voiceCommand.intentClassification,
                        response = voiceCommand.responseGenerated,
                        confidence = voiceCommand.confidenceScore,
                        wasSuccessful = voiceCommand.wasSuccessful,
                        timestamp = voiceCommand.timestamp
                    )
                }
                
                // Speak the response
                if (voiceCommand.wasSuccessful) {
                    NotificationHelper.speakHealthcareResponse(voiceCommand.responseGenerated)
                } else {
                    NotificationHelper.speakHealthcareResponse("I'm having trouble understanding. Could you rephrase your question?")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice command", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        wasSuccessful = false,
                        response = "I'm having trouble understanding. Could you rephrase your question?"
                    )
                }
                
                NotificationHelper.speakHealthcareResponse("I'm having trouble understanding. Could you rephrase your question?")
            }
        }
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening() {
        _uiState.update { it.copy(isListening = true) }
    }
    
    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        _uiState.update { it.copy(isListening = false) }
    }
    
    /**
     * Clear the current command and response
     */
    fun clearCommand() {
        _uiState.update { VoiceCommandUiState() }
    }
    
    companion object {
        private const val TAG = "VoiceCommandViewModel"
    }
}