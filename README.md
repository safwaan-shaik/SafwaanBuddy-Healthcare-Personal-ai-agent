# SafwaanBuddy Healthcare - Android App

## Overview

SafwaanBuddy Healthcare is a comprehensive Android application designed to provide personalized healthcare support with a caring, human-like conversational AI companion. This app transforms traditional healthcare management into an empathetic, interactive experience.

## Key Features

### 🎯 Human-Like Conversational AI
- **Voice Integration**: Text-to-Speech (TTS) and Speech-to-Text (STT) capabilities
- **Conversational AI**: Integration with OpenAI and Google Gemini for intelligent responses
- **Caring Personality**: Proactive health check-ins, motivational messages, and emotional support
- **Smart Notifications**: Personalized medication reminders and health updates
- **Emotional Health Coaching**: Progress celebrations and gentle encouragement

### 💊 Healthcare Management
- Medication tracking and reminders
- Health data monitoring
- Appointment scheduling
- Lab results management
- Emergency contact system

### 🔒 Security & Privacy
- HIPAA-compliant data handling
- End-to-end encryption
- Biometric authentication
- Secure cloud backup

## Voice Assistant Features

### Voice Conversation System
- **Text-to-Speech (TTS)**: Natural, human-like voice responses
- **Speech-to-Text (STT)**: Accurate voice command recognition
- **Natural Conversation Flow**: Context-aware responses that maintain conversation history

### AI Personality Integration
- **OpenAI Integration**: Uses GPT models for healthcare-focused, empathetic responses
- **Google Gemini Integration**: Alternative AI provider for diverse response styles
- **Caring Persona**: Responses designed to be supportive, encouraging, and healthcare-focused

### Caring Notification System
- **Proactive Health Check-ins**: Scheduled messages to check on user wellbeing
- **Personalized Medication Reminders**: Context-aware medication notifications
- **Emotional Support Messages**: Motivational and supportive messages

### Emotional Health Coaching
- **Motivational Messages**: Personalized encouragement for health goals
- **Progress Celebrations**: Recognition of user achievements
- **Gentle Reminders**: Compassionate prompts for health activities
- **Follow-up Questions**: Contextual questions to maintain engagement

## Technical Implementation

### Architecture
- **Jetpack Compose**: Modern UI toolkit for native Android interfaces
- **Hilt**: Dependency injection for clean, testable code
- **Room Database**: Local data storage with LiveData and Flow
- **WorkManager**: Background task scheduling for notifications
- **Navigation Component**: Single-Activity architecture with Compose Navigation

### Dependencies
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.6")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Database (Room)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Dependency Injection (Hilt)
implementation("com.google.dagger:hilt-android:2.48")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
ksp("com.google.dagger:hilt-compiler:2.48")

// Voice Integration
implementation("androidx.speech:speech-android:1.0.0")
implementation("com.google.cloud:google-cloud-texttospeech:2.5.0")

// Conversational AI
implementation("com.aallam.openai:openai-client:3.6.2")
implementation("com.google.ai.client.generativeai:generativeai:0.1.2")

// Other
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.biometric:biometric:1.1.0")
```

### Permissions Required
```xml
<!-- Healthcare App Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

<!-- Camera and Storage for Prescription Scanning -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Biometric Authentication -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />

<!-- Audio for Voice Commands and TTS -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Google Cloud TTS Services -->
<uses-permission android:name="com.google.android.tts.permission.TTS_BINDING" />
```

## User Experience

The SafwaanBuddy app creates a caring healthcare companion that:

- Remembers health goals and progress
- Celebrates achievements with personalized messages
- Provides gentle medication reminders with emotional support
- Asks about wellbeing proactively
- Offers emotional support during the healthcare journey

### Example Interactions

1. **Medication Reminders**:
   - "Don't forget to take your prenatal vitamins! You're doing a great job staying on track."
   - "I noticed you missed your iron supplement yesterday. Is everything okay?"

2. **Health Check-ins**:
   - "Hey Sarah, how's your blood pressure today?"
   - "How are you feeling this week? Any concerns I can help with?"

3. **Motivational Messages**:
   - "Great job taking all your medications this week! You're being so proactive about your health."
   - "You're in the home stretch! Just a few more weeks to go - you've got this!"

4. **Follow-up Questions**:
   - "How did that doctor's appointment go yesterday?"
   - "Did you have any morning sickness today?"

## Setup Instructions

### 1. API Keys Configuration

To use the conversational AI features, you need to configure API keys:

1. **OpenAI API Key**: 
   - Sign up at [OpenAI Platform](https://platform.openai.com/)
   - Generate an API key
   - Add to `VoiceModule.kt`:
     ```kotlin
     @Provides
     @Named("openAiApiKey")
     fun provideOpenAiApiKey(): String {
         return "YOUR_OPENAI_API_KEY_HERE"
     }
     ```

2. **Google Gemini API Key**:
   - Sign up at [Google AI Studio](https://aistudio.google.com/)
   - Generate an API key
   - Add to `VoiceModule.kt`:
     ```kotlin
     @Provides
     @Named("geminiApiKey")
     fun provideGeminiApiKey(): String {
         return "YOUR_GEMINI_API_KEY_HERE"
     }
     ```

### 2. Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Add your API keys as described above
5. Build and run the application

### 3. GitHub Actions Workflow

The project includes a GitHub Actions workflow for automated building:

```yaml
name: Android Build

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug --stacktrace
      
    - name: Build Release APK
      run: ./gradlew assembleRelease --stacktrace
      
    - name: Upload Debug APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
        
    - name: Upload Release APK
      uses: actions/upload-artifact@v3
      with:
        name: release-apk
        path: app/build/outputs/apk/release/app-release-unsigned.apk
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues or questions about the implementation, please contact the development team or refer to the documentation in the source code.