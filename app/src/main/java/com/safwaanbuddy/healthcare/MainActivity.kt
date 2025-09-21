package com.safwaanbuddy.healthcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.safwaanbuddy.healthcare.navigation.AppNavigation
import com.safwaanbuddy.healthcare.notification.NotificationHelper
import com.safwaanbuddy.healthcare.notification.NotificationScheduler
import com.safwaanbuddy.healthcare.ui.theme.SafwaanBuddyTheme
import com.safwaanbuddy.healthcare.voice.HealthcareVoiceService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for SafwaanBuddy Healthcare Application
 * Entry point for the healthcare companion app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize notification system
        NotificationHelper.createNotificationChannel(this)
        NotificationScheduler.scheduleHealthCheckWorker(this)
        
        setContent {
            SafwaanBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * Welcome screen for healthcare application
 */
@Composable
fun HealthcareWelcomeScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Welcome to SafwaanBuddy Healthcare!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HealthcareWelcomePreview() {
    SafwaanBuddyTheme {
        HealthcareWelcomeScreen()
    }
}