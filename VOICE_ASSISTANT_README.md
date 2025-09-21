# SafwaanBuddy Healthcare Voice Assistant

## Overview

The SafwaanBuddy Healthcare Voice Assistant is a comprehensive conversational AI system that transforms your healthcare app into a caring, empathetic companion. This system provides:

- **Voice Integration**: Text-to-Speech (TTS) and Speech-to-Text (STT) capabilities
- **Conversational AI**: Integration with OpenAI and Google Gemini for intelligent responses
- **Caring Personality**: Proactive health check-ins, motivational messages, and emotional support
- **Smart Notifications**: Personalized medication reminders and health updates
- **Emotional Health Coaching**: Progress celebrations and gentle encouragement

## Features Implemented

### 1. Voice Conversation System

- **Text-to-Speech (TTS)**: The app can speak responses to users using Android's built-in TTS engine
- **Speech-to-Text (STT)**: The app can listen to and understand user voice commands
- **Natural Conversation Flow**: Context-aware responses that maintain conversation history

### 2. AI Personality Integration

- **OpenAI Integration**: Uses GPT models for healthcare-focused, empathetic responses
- **Google Gemini Integration**: Alternative AI provider for diverse response styles
- **Caring Persona**: Responses are designed to be supportive, encouraging, and healthcare-focused

### 3. Caring Notification System

- **Proactive Health Check-ins**: Scheduled messages to check on user wellbeing
- **Personalized Medication Reminders**: Context-aware medication notifications
- **Emotional Support Messages**: Motivational and supportive messages

### 4. Emotional Health Coaching

- **Motivational Messages**: Personalized encouragement for health goals
- **Progress Celebrations**: Recognition of user achievements
- **Gentle Reminders**: Compassionate prompts for health activities
- **Follow-up Questions**: Contextual questions to maintain engagement

## Technical Implementation

### Dependencies Added

```kotlin
// Voice Integration
implementation("androidx.speech:speech-android:1.0.0")
implementation("com.google.cloud:google-cloud-texttospeech:2.5.0")

// Conversational AI
implementation("com.aallam.openai:openai-client:3.6.2")
implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
```

### Key Components

1. **HealthcareAiService**: Core AI service that handles communication with OpenAI and Gemini
2. **VoiceHealthcareAssistant**: Manages voice recognition and text-to-speech functionality
3. **HealthcareNotificationWorker**: Background worker for sending caring notifications
4. **VoiceInteractionActivity**: UI activity for voice command interactions
5. **VoiceInteractionScreen**: Composable UI for voice interactions
6. **HealthcareTtsService**: Custom TTS service implementation
7. **HealthcareSttService**: Custom STT service implementation

### Permissions Required

```xml
<!-- Audio for Voice Commands and TTS -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Google Cloud TTS Services -->
<uses-permission android:name="com.google.android.tts.permission.TTS_BINDING" />
```

## User Experience

The voice assistant creates a caring healthcare companion that:

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

### 2. Gradle Setup

Ensure the following dependencies are in your `build.gradle.kts`:

```kotlin
dependencies {
    // Voice Integration
    implementation("androidx.speech:speech-android:1.0.0")
    implementation("com.google.cloud:google-cloud-texttospeech:2.5.0")
    
    // Conversational AI
    implementation("com.aallam.openai:openai-client:3.6.2")
    implementation("com.google.ai.client.generativeai:generativeai:0.1.2")
}
```

### 3. AndroidManifest Configuration

Ensure the following permissions and services are declared in your `AndroidManifest.xml`:

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="com.google.android.tts.permission.TTS_BINDING" />

<!-- Services -->
<service
    android:name=".voice.HealthcareTtsService"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.TTS_SERVICE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
    <meta-data
        android:name="android.speech.tts.engine.TtsService"
        android:resource="@xml/tts_settings" />
</service>

<service
    android:name=".voice.HealthcareSttService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.safwaanbuddy.healthcare.VOICE_RECOGNITION" />
    </intent-filter>
</service>
```

## Usage Guide

### Accessing the Voice Assistant

1. Open the SafwaanBuddy app
2. Navigate to the Dashboard
3. Tap the "Voice Assistant" button in the Quick Actions section
4. Use the microphone button to start speaking
5. Receive caring, personalized healthcare responses

### Voice Commands

The voice assistant understands various healthcare-related queries:

- **Medication-related**: "When is my next dose?", "Did I take my vitamins?"
- **Health status**: "How's my blood pressure?", "Am I gaining too much weight?"
- **Appointments**: "When is my next doctor visit?"
- **Lab results**: "Do I have any new test results?"
- **General health**: "How can I feel better during pregnancy?"

## Customization

### Personality Tuning

The AI personality can be customized in `HealthcareAiService.kt` by modifying the `healthcarePrompt`:

```kotlin
private val healthcarePrompt = """
    You are SafwaanBuddy, a caring healthcare companion for pregnant women.
    
    You should always respond with:
    - Empathy and compassion
    - Healthcare knowledge
    - Proactive check-ins
    - Gentle reminders
    - Motivational messages
"""
```

### Notification Scheduling

Adjust notification frequency in `HealthcareNotificationWorker.kt`:

```kotlin
// Modify the work request constraints
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

val workRequest = PeriodicWorkRequestBuilder<HealthcareNotificationWorker>(
    8, TimeUnit.HOURS) // Adjust frequency here
    .setConstraints(constraints)
    .build()
```

## Privacy and Security

- All voice interactions are processed locally when possible
- AI conversations are HIPAA-compliant
- No personal health data is stored on external servers without encryption
- Users can delete voice interaction logs at any time

## Troubleshooting

### Common Issues

1. **Voice recognition not working**:
   - Ensure microphone permissions are granted
   - Check that device has internet connection
   - Verify Google Services are up to date

2. **TTS not speaking**:
   - Check device volume settings
   - Ensure TTS engine is properly installed
   - Verify language settings match device locale

3. **AI responses not loading**:
   - Confirm API keys are correctly configured
   - Check internet connectivity
   - Verify API quotas have not been exceeded

### Logs and Debugging

Enable debug logging by setting log levels in:
- `HealthcareAiService.kt`
- `VoiceHealthcareAssistant.kt`
- `HealthcareNotificationWorker.kt`

## Future Enhancements

Planned improvements include:
- Multilingual support
- Voice customization options
- Integration with wearable health devices
- Advanced emotion detection from voice tone
- Personalized care plans based on user history

## Support

For issues or questions about the voice assistant implementation, please contact the development team or refer to the documentation in the source code.