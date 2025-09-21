package com.safwaanbuddy.healthcare.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.safwaanbuddy.healthcare.voice.VoiceInteractionScreen

@Composable
fun VoiceNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "voice_interaction"
    ) {
        composable("voice_interaction") {
            VoiceInteractionScreen()
        }
    }
}