package com.nurturepk.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nurturepk.data.Reminder
import com.nurturepk.data.ReminderType
import kotlinx.datetime.*

/**
 * Dialog for adding new reminders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ReminderType.MEDICATION) }
    var isRecurring by remember { mutableStateOf(false) }
    
    // Date and time selection
    var selectedDate by remember { 
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    }
    var selectedTime by remember { 
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Reminder",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Message Input
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                // Reminder Type Selection
                Text(
                    text = "Reminder Type",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Column(modifier = Modifier.selectableGroup()) {
                    ReminderType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedType == type),
                                    onClick = { selectedType = type },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedType == type),
                                onClick = null
                            )
                            Text(
                                text = when (type) {
                                    ReminderType.MEDICATION -> "Medication"
                                    ReminderType.NUTRITION -> "Nutrition"
                                    ReminderType.ACTIVITY -> "Activity"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                
                // Recurring Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text(
                        text = "Recurring reminder",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // Date Selection (Simplified for now)
                Text(
                    text = "Scheduled Date: ${selectedDate}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Scheduled Time: ${selectedTime}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val scheduledDateTime = LocalDateTime(selectedDate, selectedTime)
                                val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                
                                val reminder = Reminder(
                                    title = title,
                                    message = message,
                                    reminderType = selectedType,
                                    scheduledTime = scheduledDateTime,
                                    isRecurring = isRecurring,
                                    isActive = true,
                                    createdAt = currentTime,
                                    updatedAt = currentTime
                                )
                                onSave(reminder)
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}