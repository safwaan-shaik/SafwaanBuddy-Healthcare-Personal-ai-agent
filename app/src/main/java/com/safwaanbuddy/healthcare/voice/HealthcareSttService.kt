// Healthcare Speech-to-Text Service
// Background service for handling speech recognition

package com.safwaanbuddy.healthcare.voice

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.*

class HealthcareSttService : Service() {
    
    companion object {
        private const val TAG = "HealthcareSttService"
    }
    
    private val binder = SttBinder()
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    
    inner class SttBinder : Binder() {
        fun getService(): HealthcareSttService = this@HealthcareSttService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeSpeechRecognizer()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
    
    private fun initializeSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            Log.d(TAG, "Speech recognizer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognizer", e)
        }
    }
    
    fun startListening(listener: RecognitionListener) {
        recognitionListener = listener
        speechRecognizer?.setRecognitionListener(listener)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.startListening(intent)
        Log.d(TAG, "Started listening for speech")
    }
    
    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "Stopped listening for speech")
    }
    
    private fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        recognitionListener = null
    }
}