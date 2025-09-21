// Healthcare AI Service Module
// Provides dependency injection for the AI service

package com.safwaanbuddy.healthcare.voice

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthcareAiServiceModule {
    
    @Provides
    @Singleton
    fun provideHealthcareAiService(
        @ApplicationContext context: Context,
        @Named("openai_api_key") openAiApiKey: String,
        @Named("gemini_api_key") geminiApiKey: String
    ): HealthcareAiService {
        return HealthcareAiService(context, openAiApiKey, geminiApiKey)
    }
}