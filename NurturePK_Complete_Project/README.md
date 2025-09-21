# NurturePK - Android Pregnancy Companion App

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Material%20Design-757575?style=for-the-badge&logo=material-design&logoColor=white" alt="Material Design">
</p>

## 📱 Overview

NurturePK is a comprehensive Android application designed to assist pregnant women in managing their health routines through intelligent reminder scheduling. The app focuses on medication reminders, nutritional guidance, and activity scheduling with a premium user interface built using modern Android development practices.

## ✨ Key Features

- 🔔 **Smart Reminder Scheduling** - Set reminders for medications, nutrition, and activities
- 📱 **Modern UI** - Built with Jetpack Compose and Material Design 3
- 💾 **Offline Support** - Local database storage for offline functionality
- ⏰ **Background Processing** - Persistent notifications using WorkManager
- 🔄 **Boot Recovery** - Maintains reminders across device restarts
- 🎨 **Premium Design** - Beautiful, intuitive user interface

## 🏗️ Architecture

### Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Room database
- **Background Tasks**: WorkManager
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

### Project Structure

```
app/src/main/java/com/nurturepk/
├── MainActivity.kt                 # Main entry point
├── data/                          # Data layer
│   ├── Reminder.kt               # Entity model
│   ├── ReminderDao.kt            # Database access object
│   ├── ReminderDatabase.kt       # Room database configuration
│   └── ReminderRepository.kt     # Repository pattern
├── ui/                           # Presentation layer
│   ├── ReminderListScreen.kt     # Main screen component
│   ├── AddReminderScreen.kt      # Add reminder dialog
│   └── theme/                    # Material Design theming
├── utils/                        # Utility classes
│   ├── NotificationHelper.kt     # Notification management
│   └── ReminderScheduler.kt      # Background task scheduling
├── worker/                       # Background workers
│   └── ReminderWorker.kt         # WorkManager task executor
└── receiver/                     # Broadcast receivers
    └── BootCompleteReceiver.kt   # Device boot handler
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 17 or higher
- Android SDK API Level 34
- Gradle 8.1.4 or higher

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/NurturePK.git
   cd NurturePK_Complete_Project
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the `NurturePK_Complete_Project` folder

3. **Sync Project**
   - Wait for Gradle sync to complete
   - Resolve any dependency issues if prompted

4. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press `Shift + F10`

## 🔨 Building APK

### Using Android Studio
1. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Using Command Line

#### Windows
```cmd
gradlew.bat clean assembleDebug
```

#### Linux/Mac
```bash
./gradlew clean assembleDebug
```

### Using Build Scripts
We've provided convenient build scripts:

#### Windows
```cmd
build.bat
```

#### Linux/Mac
```bash
chmod +x build.sh
./build.sh
```

## 📋 Features Detail

### Reminder Types
- **Medication Reminders**: Track dosages and timing
- **Nutrition Reminders**: Meal planning and vitamin schedules
- **Activity Reminders**: Exercise routines and medical appointments

### Notification System
- High-priority notifications for medications
- Custom notification channels for different reminder types
- Persistent notifications that survive device restarts
- Rich notification content with custom actions

### Database
- Local-first approach using Room database
- Offline functionality
- Automatic data backup capabilities
- Efficient querying and indexing

## 🧪 Testing

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Running Instrumented Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Test Coverage
- Database operations testing
- WorkManager background task verification
- UI component testing with Compose Testing
- Notification system validation

## 🔧 Configuration

### Permissions
The app requires the following permissions:
- `POST_NOTIFICATIONS` - For displaying reminders
- `WAKE_LOCK` - For background processing
- `RECEIVE_BOOT_COMPLETED` - For scheduling after reboot
- `VIBRATE` - For notification vibration
- `SCHEDULE_EXACT_ALARM` - For precise timing

### Notification Channels
- **Medication Channel**: High priority with sound and vibration
- **Nutrition Channel**: Default priority
- **Activity Channel**: Default priority

## 🚀 Deployment

### Debug Build
- Package: `com.nurturepk.debug`
- Debuggable: True
- Minification: Disabled

### Release Build
- Package: `com.nurturepk`
- Debuggable: False
- Minification: Enabled (ProGuard)
- Resource Shrinking: Enabled

## 🔄 CI/CD

The project includes GitHub Actions workflow for:
- Automated building and testing
- APK generation for multiple API levels
- Instrumented testing on emulators
- Automatic releases on main branch

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🛠️ Troubleshooting

### Common Build Issues

1. **Gradle Sync Failed**
   ```bash
   ./gradlew clean --refresh-dependencies
   ```

2. **Room Schema Export Error**
   - Ensure schema export directory exists
   - Add proper KSP configuration

3. **WorkManager Initialization**
   - Verify WorkManager initialization in Application class
   - Check manifest configuration

4. **Notification Permission Issues**
   - Target SDK 33+ requires runtime notification permission
   - Handle permission requests properly in UI

## 📞 Support

For support and questions:
- Create an issue on GitHub
- Contact: [your-email@domain.com]

## 🎯 Roadmap

- [ ] Cloud sync functionality
- [ ] Biometric authentication
- [ ] Advanced recurring patterns
- [ ] Integration with health APIs
- [ ] Multi-language support
- [ ] Dark theme optimization

---

<p align="center">
  Made with ❤️ for expecting mothers
</p>