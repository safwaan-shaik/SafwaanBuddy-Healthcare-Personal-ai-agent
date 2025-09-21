package com.safwaanbuddy.healthcare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safwaanbuddy.healthcare.ui.theme.*

/**
 * Medication Management Screen
 * Shows active medications, reminders, and compliance tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Text(
                text = "Medications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compliance Summary Card
            item {
                ComplianceSummaryCard(
                    complianceRate = uiState.complianceRate,
                    missedDoses = uiState.missedDoses,
                    onViewDetails = { /* Navigate to compliance details */ }
                )
            }
            
            // Active Medications Section
            item {
                SectionHeader(
                    title = "Active Medications",
                    subtitle = "${uiState.activeMedications.size} medications"
                )
            }
            
            items(uiState.activeMedications) { medication ->
                MedicationCard(
                    medication = medication,
                    onTakeNow = { viewModel.markMedicationTaken(medication.id) },
                    onSnooze = { viewModel.snoozeMedication(medication.id) },
                    onViewDetails = { /* Navigate to medication details */ }
                )
            }
            
            // Add spacing at bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Floating Action Button for adding medications
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { /* Navigate to add medication */ },
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Medication"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComplianceSummaryCard(
    complianceRate: Float,
    missedDoses: Int,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onViewDetails,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                complianceRate >= 90f -> HealthSafe.copy(alpha = 0.1f)
                complianceRate >= 70f -> HealthCaution.copy(alpha = 0.1f)
                else -> HealthDanger.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medication Compliance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                
                Icon(
                    imageVector = when {
                        complianceRate >= 90f -> Icons.Default.CheckCircle
                        complianceRate >= 70f -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = "Compliance status",
                    tint = when {
                        complianceRate >= 90f -> HealthSafe
                        complianceRate >= 70f -> HealthCaution
                        else -> HealthDanger
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${complianceRate.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            complianceRate >= 90f -> HealthSafe
                            complianceRate >= 70f -> HealthCaution
                            else -> HealthDanger
                        }
                    )
                    
                    Text(
                        text = "Compliance Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$missedDoses",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (missedDoses > 0) HealthDanger else HealthSafe
                    )
                    
                    Text(
                        text = "Missed Doses",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    complianceRate >= 95f -> "Excellent! Keep up the great work."
                    complianceRate >= 85f -> "Good adherence to your medication schedule."
                    complianceRate >= 70f -> "Consider setting more reminders to improve compliance."
                    else -> "Please discuss your medication schedule with your healthcare provider."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationCard(
    medication: MedicationUiModel,
    onTakeNow: () -> Unit,
    onSnooze: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onViewDetails,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "${medication.dosage} • ${medication.frequency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (medication.instructions.isNotEmpty()) {
                        Text(
                            text = medication.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Safety indicator
                SafetyIndicator(
                    safetyCategory = medication.safetyCategory,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Next dose info
            if (medication.nextDoseTime.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Next dose",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Next dose: ${medication.nextDoseTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action buttons
            if (medication.isDue) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Snooze",
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text("Snooze")
                    }
                    
                    Button(
                        onClick = onTakeNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Take now",
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text("Take Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyIndicator(
    safetyCategory: String,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (safetyCategory) {
        "safe" -> Triple(HealthSafe, Icons.Default.CheckCircle, "Safe")
        "caution" -> Triple(HealthCaution, Icons.Default.Warning, "Caution")
        "avoid" -> Triple(HealthDanger, Icons.Default.Error, "Avoid")
        else -> Triple(HealthUnknown, Icons.Default.Help, "Unknown")
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

// Data classes for Medication UI
data class MedicationUiState(
    val activeMedications: List<MedicationUiModel> = emptyList(),
    val complianceRate: Float = 0f,
    val missedDoses: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class MedicationUiModel(
    val id: Long,
    val name: String,
    val dosage: String,
    val frequency: String,
    val instructions: String,
    val nextDoseTime: String,
    val isDue: Boolean,
    val safetyCategory: String
)

// Sample ViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    // Inject repositories here
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()
    
    init {
        loadMedications()
    }
    
    private fun loadMedications() {
        viewModelScope.launch {
            // Load from repository
            val medications = listOf(
                MedicationUiModel(
                    id = 1L,
                    name = "Prenatal Vitamin",
                    dosage = "1 tablet",
                    frequency = "Once daily",
                    instructions = "Take with food in the morning",
                    nextDoseTime = "Tomorrow at 8:00 AM",
                    isDue = false,
                    safetyCategory = "safe"
                ),
                MedicationUiModel(
                    id = 2L,
                    name = "Iron Supplement",
                    dosage = "325mg",
                    frequency = "Twice daily",
                    instructions = "Take between meals",
                    nextDoseTime = "Today at 2:00 PM",
                    isDue = true,
                    safetyCategory = "safe"
                ),
                MedicationUiModel(
                    id = 3L,
                    name = "Folic Acid",
                    dosage = "400mcg",
                    frequency = "Once daily",
                    instructions = "Take with water",
                    nextDoseTime = "Tomorrow at 8:00 AM",
                    isDue = false,
                    safetyCategory = "safe"
                )
            )
            
            _uiState.value = _uiState.value.copy(
                activeMedications = medications,
                complianceRate = 94.5f,
                missedDoses = 2
            )
        }
    }
    
    fun markMedicationTaken(medicationId: Long) {
        viewModelScope.launch {
            // Update medication status
            // This would call the reminder manager
        }
    }
    
    fun snoozeMedication(medicationId: Long) {
        viewModelScope.launch {
            // Snooze medication reminder
            // This would call the reminder manager
        }
    }
}