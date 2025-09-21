// Voice Module
// Provides dependencies for voice interaction components

package com.safwaanbuddy.healthcare.voice

import android.content.Context
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {
    
    @Provides
    @Named("openAiApiKey")
    fun provideOpenAiApiKey(): String {
        // In a real implementation, this would be securely stored and retrieved
        // For now, we'll return a placeholder
        return "YOUR_OPENAI_API_KEY_HERE"
    }
    
    @Provides
    @Named("geminiApiKey")
    fun provideGeminiApiKey(): String {
        // In a real implementation, this would be securely stored and retrieved
        // For now, we'll return a placeholder
        return "YOUR_GEMINI_API_KEY_HERE"
    }
    
    @Provides
    @Singleton
    fun provideHealthcareAiService(
        @ApplicationContext context: Context,
        @Named("openAiApiKey") openAiApiKey: String,
        @Named("geminiApiKey") geminiApiKey: String
    ): HealthcareAiService {
        return HealthcareAiService(context, openAiApiKey, geminiApiKey)
    }
    
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}