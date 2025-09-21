package com.safwaanbuddy.healthcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safwaanbuddy.healthcare.ui.theme.*

/**
 * Healthcare Dashboard Screen
 * Main screen showing overview of patient health data and quick actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthcareDashboardScreen(
    onNavigateToMedications: () -> Unit,
    onNavigateToPrescriptions: () -> Unit,
    onNavigateToLabResults: () -> Unit,
    onNavigateToVoiceAssistant: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthcareDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Healthcare Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Quick Stats Cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(uiState.quickStats) { stat ->
                QuickStatCard(
                    title = stat.title,
                    value = stat.value,
                    subtitle = stat.subtitle,
                    icon = stat.icon,
                    backgroundColor = stat.backgroundColor
                )
            }
        }
        
        // Recent Activities
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Recent Activities",
                    subtitle = "Your latest health updates"
                )
            }
            
            items(uiState.recentActivities) { activity ->
                ActivityCard(
                    title = activity.title,
                    description = activity.description,
                    time = activity.time,
                    type = activity.type,
                    onClick = { /* Handle activity click */ }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick Actions
                SectionHeader(
                    title = "Quick Actions",
                    subtitle = "Manage your healthcare"
                )
            }
            
            item {
                QuickActionsGrid(
                    onMedicationsClick = onNavigateToMedications,
                    onPrescriptionsClick = onNavigateToPrescriptions,
                    onLabResultsClick = onNavigateToLabResults,
                    onVoiceAssistantClick = onNavigateToVoiceAssistant
                )
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCard(
    title: String,
    description: String,
    time: String,
    type: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Activity type icon
            Icon(
                imageVector = when (type) {
                    "medication" -> Icons.Default.LocalPharmacy
                    "lab_result" -> Icons.Default.Science
                    "prescription" -> Icons.Default.Receipt
                    "voice_command" -> Icons.Default.Mic
                    else -> Icons.Default.Info
                },
                contentDescription = type,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp),
                tint = when (type) {
                    "medication" -> HealthSafe
                    "lab_result" -> HealthCaution
                    "prescription" -> MaterialTheme.colorScheme.primary
                    "voice_command" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onMedicationsClick: () -> Unit,
    onPrescriptionsClick: () -> Unit,
    onLabResultsClick: () -> Unit,
    onVoiceAssistantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                title = "Medications",
                icon = Icons.Default.LocalPharmacy,
                onClick = onMedicationsClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                title = "Prescriptions",
                icon = Icons.Default.Receipt,
                onClick = onPrescriptionsClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                title = "Lab Results",
                icon = Icons.Default.Science,
                onClick = onLabResultsClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                title = "Voice Assistant",
                icon = Icons.Default.Mic,
                onClick = onVoiceAssistantClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Data classes for Dashboard UI State
 */
data class HealthcareDashboardUiState(
    val quickStats: List<QuickStat> = emptyList(),
    val recentActivities: List<RecentActivity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class QuickStat(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val backgroundColor: androidx.compose.ui.graphics.Color
)

data class RecentActivity(
    val title: String,
    val description: String,
    val time: String,
    val type: String
)

/**
 * Sample ViewModel for Healthcare Dashboard
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthcareDashboardViewModel @Inject constructor(
    // Inject your repositories here
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HealthcareDashboardUiState())
    val uiState: StateFlow<HealthcareDashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load dashboard data from repositories
                val quickStats = loadQuickStats()
                val recentActivities = loadRecentActivities()
                
                _uiState.value = _uiState.value.copy(
                    quickStats = quickStats,
                    recentActivities = recentActivities,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    private fun loadQuickStats(): List<QuickStat> {
        // This would load real data from repositories
        return listOf(
            QuickStat(
                title = "Active Medications",
                value = "3",
                subtitle = "All on schedule",
                icon = Icons.Default.LocalPharmacy,
                backgroundColor = HealthSafe.copy(alpha = 0.1f)
            ),
            QuickStat(
                title = "Compliance Rate",
                value = "95%",
                subtitle = "Excellent",
                icon = Icons.Default.CheckCircle,
                backgroundColor = HealthSafe.copy(alpha = 0.1f)
            ),
            QuickStat(
                title = "Next Appointment",
                value = "3",
                subtitle = "Days away",
                icon = Icons.Default.Schedule,
                backgroundColor = HealthCaution.copy(alpha = 0.1f)
            )
        )
    }
    
    private fun loadRecentActivities(): List<RecentActivity> {
        // This would load real data from repositories
        return listOf(
            RecentActivity(
                title = "Prenatal Vitamin Taken",
                description = "Morning dose completed successfully",
                time = "2 hours ago",
                type = "medication"
            ),
            RecentActivity(
                title = "Lab Results Available",
                description = "Blood work results are ready for review",
                time = "Yesterday",
                type = "lab_result"
            ),
            RecentActivity(
                title = "New Prescription Added",
                description = "Iron supplement prescription processed",
                time = "2 days ago",
                type = "prescription"
            ),
            RecentActivity(
                title = "Voice Command Used",
                description = "Asked about medication schedule",
                time = "3 days ago",
                type = "voice_command"
            )
        )
    }
}