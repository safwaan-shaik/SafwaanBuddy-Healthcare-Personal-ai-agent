package com.safwaanbuddy.healthcare.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safwaanbuddy.healthcare.ui.screens.HealthcareDashboardScreen
import com.safwaanbuddy.healthcare.ui.screens.MedicationScreen
import com.safwaanbuddy.healthcare.voice.VoiceInteractionScreen

/**
 * App Navigation Component
 * Handles navigation between different screens in the healthcare app
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavigationDestination.Dashboard.route
    ) {
        composable(NavigationDestination.Dashboard.route) {
            HealthcareDashboardScreen(
                onNavigateToMedications = {
                    navController.navigate(NavigationDestination.Medications.route)
                },
                onNavigateToPrescriptions = {
                    // Navigate to prescriptions screen when implemented
                },
                onNavigateToLabResults = {
                    // Navigate to lab results screen when implemented
                },
                onNavigateToVoiceAssistant = {
                    navController.navigate(NavigationDestination.VoiceAssistant.route)
                }
            )
        }
        
        composable(NavigationDestination.Medications.route) {
            MedicationScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(NavigationDestination.VoiceAssistant.route) {
            VoiceInteractionScreen()
        }
    }
}

/**
 * Navigation destinations for the healthcare app
 */
sealed class NavigationDestination(val route: String, val title: String) {
    object Dashboard : NavigationDestination("dashboard", "Dashboard")
    object Medications : NavigationDestination("medications", "Medications")
    object VoiceAssistant : NavigationDestination("voice_assistant", "Voice Assistant")
}