// Voice Interaction Screen
// Composable UI for voice commands and responses

package com.safwaanbuddy.healthcare.voice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safwaanbuddy.healthcare.ui.theme.*
import kotlinx.coroutines.*

/**
 * Voice Interaction Screen
 * Composable UI for voice commands and responses
 */
@Composable
fun VoiceInteractionScreen(
    viewModel: VoiceInteractionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    VoiceInteractionScreenContent(
        isListening = uiState.isListening,
        isProcessing = uiState.isProcessing,
        command = uiState.command,
        response = uiState.response,
        confidence = uiState.confidence,
        onToggleListening = { viewModel.toggleListening(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    viewModel.processVoiceCommand(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }) },
        onClear = { viewModel.clearInteraction() }
    )
}

@Composable
fun VoiceInteractionScreenContent(
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    command: String = "",
    response: String = "",
    confidence: Float = 0.0f,
    onToggleListening: () -> Unit = {},
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Title
        Text(
            text = "Voice Assistant",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Status indicator
        if (isProcessing) {
            ProcessingIndicator()
        } else if (isListening) {
            ListeningIndicator()
        } else {
            // Microphone icon
            IconButton(
                onClick = onToggleListening,
                modifier = Modifier
                    .size(64.dp)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = if (command.isEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Gray
                    }
                )
            }
        }
        
        // Command section
        if (command.isNotEmpty()) {
            Section(
                title = "Command",
                content = command,
                color = MaterialTheme.colorScheme.primary.copy(0.1f)
            )
            
            // Confidence indicator
            ConfidenceIndicator(confidence)
        }
        
        // Response section
        if (response.isNotEmpty()) {
            Section(
                title = "Response",
                content = response,
                color = MaterialTheme.colorScheme.secondary.copy(0.1f)
            )
        }
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear button
            if (command.isNotEmpty() || response.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.Gray
                    )
                }
            }
            
            // Toggle listening button
            Button(
                onClick = onToggleListening,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isListening) "Stop" else "Start",
                    color = Color.White
                )
            }
        }
        
        // Instructions
        if (command.isEmpty() && response.isEmpty() && !isListening && !isProcessing) {
            Text(
                text = "Tap the microphone to ask about medications, health status, lab results, or get emergency assistance",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ListeningIndicator() {
    // Animated listening indicator
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated microphone
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Listening...",
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .padding(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Listening...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Speak now - I'm ready to help",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun ProcessingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Progress indicator
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Processing your request...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Text(
            text = "Please wait a moment",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun ConfidenceIndicator(confidence: Float) {
    val color = when {
        confidence > 0.8f -> Color.Green
        confidence > 0.6f -> Color.Yellow
        confidence > 0.4f -> Color.Orange
        else -> Color.Red
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Confidence: ${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // Confidence bar
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(12.dp)
                .background(Color.LightGray)
                .clip(RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .width((150 * confidence).dp)
                    .height(12.dp)
                    .background(color)
                    .clip(RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun Section(title: String, content: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(color, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Section title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Section content
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}