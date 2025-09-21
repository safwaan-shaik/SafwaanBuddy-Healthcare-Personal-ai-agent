// Healthcare Text-to-Speech Service
// Provides voice responses for the healthcare app

package com.safwaanbuddy.healthcare.voice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.safwaanbuddy.healthcare.SafwaanBuddyApplication
import com.safwaanbuddy.healthcare.data.models.VoiceCommandLog
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HealthcareTtsService : Service() {
    
    private lateinit var textToSpeech: TextToSpeech
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var application: SafwaanBuddyApplication
    
    override fun onCreate() {
        super.onCreate()
        application = applicationContext as SafwaanBuddyApplication
        
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch { 
                            // Notify that speaking has started
                            application.voiceCallback?.onSpeechStarted()
                        }
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        scope.launch {
                            // Notify that speaking has finished
                            application.voiceCallback?.onSpeechEnded()
                            
                            // Log the utterance
                            utteranceId?.let { id ->
                                val voiceLog = VoiceCommandLog(
                                    commandText = "TTS: $id",
                                    intentClassification = "TTS",
                                    responseGenerated = id,
                                    confidenceScore = 1.0f,
                                    processingTimeMs = 0L,
                                    wasSuccessful = true,
                                    timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                )
                                
                                application.database.voiceCommandLogDao().insertVoiceCommand(voiceLog)
                            }
                        }
                    }
                    
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS Error: $errorCode")
                        scope.launch {
                            application.voiceCallback?.onError("Text-to-speech error: $errorCode")
                        }
                    }
                }
            } else {
                Log.e(TAG, "Text-to-speech initialization failed")
                scope.launch {
                    application.voiceCallback?.onError("Text-to-speech initialization failed")
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
    
    companion object {
        private const val TAG = "HealthcareTtsService"
    }
}

// Extension function to speak healthcare responses
fun TextToSpeech.speakHealthcare(text: String, utteranceId: String = "healthcare_response") {
    this.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
}

// Extension function to initialize healthcare TTS
fun initializeHealthcareTts(context: Context, callback: VoiceCallback) {
    val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    callback.onSpeechStarted()
                }
                
                override fun onDone(utteranceId: String?) {
                    callback.onSpeechEnded()
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS Error: $errorCode")
                    callback.onError("Text-to-speech error: $errorCode")
                }
            }
        } else {
            Log.e(TAG, "Text-to-speech initialization failed")
            callback.onError("Text-to-speech initialization failed")
        }
    }
    
    // Return the initialized TextToSpeech instance
    tts
}