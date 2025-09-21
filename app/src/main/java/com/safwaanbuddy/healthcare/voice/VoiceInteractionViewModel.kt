package com.safwaanbuddy.healthcare.voice

import android.content.Context
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safwaanbuddy.healthcare.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationMessage(
    val text: String,
    val sender: MessageSender
)

enum class MessageSender {
    USER, AI
}

data class VoiceInteractionUiState(
    val messages: List<ConversationMessage> = listOf(
        ConversationMessage(
            "Hello! I'm your healthcare companion. How are you feeling today?",
            MessageSender.AI
        )
    ),
    val isListening: Boolean = false,
    val status: String = "Ready to listen"
)

@HiltViewModel
class VoiceInteractionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthcareAiService: HealthcareAiService,
    private val patientRepository: PatientRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VoiceInteractionUiState())
    val uiState: StateFlow<VoiceInteractionUiState> = _uiState.asStateFlow()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentPatientId: Long = 1L // Default patient ID
    
    fun initializeVoiceServices() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.value = _uiState.value.copy(status = "Listening...")
            }
            
            override fun onBeginningOfSpeech() {
                _uiState.value = _uiState.value.copy(status = "Processing speech...")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Not used
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }
            
            override fun onEndOfSpeech() {
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    status = "Processing your request..."
                )
            }
            
            override fun onError(error: Int) {
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    status = "Error occurred. Please try again."
                )
                Log.e("VoiceInteraction", "Speech recognition error: $error")
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.get(0) ?: ""
                
                if (spokenText.isNotEmpty()) {
                    // Add user message to conversation
                    val currentMessages = _uiState.value.messages.toMutableList()
                    currentMessages.add(ConversationMessage(spokenText, MessageSender.USER))
                    _uiState.value = _uiState.value.copy(messages = currentMessages)
                    
                    // Process with AI
                    processUserInput(spokenText)
                }
                
                _uiState.value = _uiState.value.copy(
                    isListening = false,
                    status = "Ready to listen"
                )
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Not used
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        })
        
        // Load current patient
        viewModelScope.launch {
            try {
                val patients = patientRepository.getAllPatients()
                if (patients.isNotEmpty()) {
                    currentPatientId = patients[0].id
                }
            } catch (e: Exception) {
                Log.e("VoiceInteraction", "Error loading patient", e)
            }
        }
    }
    
    fun startListening() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        _uiState.value = _uiState.value.copy(
            isListening = true,
            status = "Listening... Please speak"
        )
        
        speechRecognizer?.startListening(intent)
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            status = "Stopped listening"
        )
    }
    
    private fun processUserInput(input: String) {
        viewModelScope.launch {
            try {
                // Get AI response
                val aiResponse = healthcareAiService.processHealthcareQuery(
                    input, 
                    currentPatientId
                ).await()
                
                // Add AI response to conversation
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(ConversationMessage(aiResponse, MessageSender.AI))
                _uiState.value = _uiState.value.copy(messages = currentMessages)
                
                // Speak the response
                HealthcareVoiceService.speak(aiResponse)
                
                _uiState.value = _uiState.value.copy(status = "Response delivered")
            } catch (e: Exception) {
                Log.e("VoiceInteraction", "Error processing user input", e)
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(
                    ConversationMessage(
                        "I'm sorry, I encountered an error processing your request. Please try again.",
                        MessageSender.AI
                    )
                )
                _uiState.value = _uiState.value.copy(
                    messages = currentMessages,
                    status = "Error occurred"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}