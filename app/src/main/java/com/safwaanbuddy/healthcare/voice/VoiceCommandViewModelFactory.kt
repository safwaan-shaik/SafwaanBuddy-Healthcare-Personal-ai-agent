// Voice Command View Model Factory
// Provides factory methods for creating view models

package com.safwaanbuddy.healthcare.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import kotlinx.coroutines.*
import java.util.*

class VoiceCommandViewModelFactory(
    private val voiceCommandRepository: VoiceCommandRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceCommandViewModel::class.java)) {
            return VoiceCommandViewModel(voiceCommandRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}