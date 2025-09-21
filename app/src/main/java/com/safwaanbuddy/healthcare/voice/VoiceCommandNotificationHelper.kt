// Voice Command Notification Helper
// Provides helper functions for voice command notifications

package com.safwaanbuddy.healthcare.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safwaanbuddy.healthcare.MainActivity
import com.safwaanbuddy.healthcare.R
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*

class VoiceCommandNotificationHelper {
    
    companion object {
        private const val TAG = "VoiceCommandNotificationHelper"
        private const val CHANNEL_ID = "voice_commands"
        private const val NOTIFICATION_ID = 1001
        
        /**
         * Create a notification channel for voice commands
         * @param context The application context
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Voice Commands"
                val description = "Notifications for voice command processing"
                val importance = NotificationManager.IMPORTANCE_HIGH
                
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    this.description = description
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    enableVibration(true)
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
        
        /**
         * Show a notification that a voice command was received
         * @param context The application context
         * @param command The voice command text
         */
        fun showVoiceCommandReceived(context: Context, command: String) {
            createNotificationChannel(context)
            
            // Create an intent to open the app when the notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_active)
                .setContentTitle("Voice Command Received")
                .setContentText("I heard: $command")
                .setStyle(NotificationCompat.BigTextStyle().bigText("I heard: $command"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        
        /**
         * Show a notification that a voice command was processed
         * @param context The application context
         * @param response The response to the voice command
         */
        fun showVoiceCommandProcessed(context: Context, response: String) {
            createNotificationChannel(context)
            
            // Create an intent to open the app when the notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_active)
                .setContentTitle("Voice Command Processed")
                .setContentText(response)
                .setStyle(NotificationCompat.BigTextStyle().bigText(response))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
        }
        
        /**
         * Show a notification that a voice command was added to the database
         * @param context The application context
         */
        fun showVoiceCommandAdded(context: Context) {
            createNotificationChannel(context)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_active)
                .setContentTitle("Voice Command Added")
                .setContentText("Your voice command has been processed and saved.")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 2, notification)
        }
        
        /**
         * Show a notification with a partial voice command result
         * @param context The application context
         * @param partialText The partial text result
         */
        fun showVoiceCommandPartialResult(context: Context, partialText: String) {
            createNotificationChannel(context)
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic_active)
                .setContentTitle("Voice Command Processing")
                .setContentText("I heard: $partialText")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .build()
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 3, notification)
        }
        
        /**
         * Speak a healthcare response
         * @param context The application context
         * @param response The response to speak
         */
        fun speakHealthcareResponse(context: Context, response: String) {
            // This would use the TextToSpeech service to speak the response
            // Implementation would be in the TTS service
            Log.d(TAG, "Speaking: $response")
        }
    }
}