# Developer Setup Guide

## Prerequisites

Before you begin, ensure you have the following installed:

### Required Software
- **Android Studio**: Arctic Fox (2020.3.1) or newer
- **JDK**: Version 17 or higher
- **Git**: For version control

### Android SDK Components
- **Android SDK**: API Level 34 (Android 14)
- **Android SDK Build-Tools**: 34.0.0
- **Android Emulator**: For testing (optional)

## Environment Setup

### 1. Install Android Studio
1. Download from [developer.android.com](https://developer.android.com/studio)
2. Follow the installation wizard
3. Install SDK components when prompted

### 2. Configure Environment Variables
Add the following to your system PATH:
```bash
# Windows
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools

# Linux/Mac
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### 3. Verify Installation
```bash
# Check Android SDK
adb version

# Check Java version
java -version

# Check Gradle
./gradlew --version
```

## Project Setup

### 1. Clone Repository
```bash
git clone https://github.com/yourusername/NurturePK.git
cd NurturePK_Complete_Project
```

### 2. Import in Android Studio
1. Open Android Studio
2. Click "Open an existing Android Studio project"
3. Navigate to `NurturePK_Complete_Project` folder
4. Click "OK"

### 3. Sync Project
1. Wait for Gradle sync to complete
2. Resolve any dependency conflicts
3. Download missing SDK components if prompted

## Building the Project

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Clean Build
```bash
./gradlew clean assembleDebug
```

## Running Tests

### Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Generate Test Reports
```bash
./gradlew testDebugUnitTest
# Reports available at: app/build/reports/tests/testDebugUnitTest/
```

## Debugging

### Enable Debug Mode
1. Open `app/build.gradle.kts`
2. Ensure debug build type has `isDebuggable = true`
3. Install debug APK on device

### Useful Debug Commands
```bash
# View device logs
adb logcat

# Filter logs by tag
adb logcat -s "NurturePK"

# Clear app data
adb shell pm clear com.nurturepk.debug
```

## Code Style

### Kotlin Style Guide
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters

### Android Best Practices
- Follow [Android Architecture Guide](https://developer.android.com/jetpack/guide)
- Use MVVM architecture pattern
- Implement proper separation of concerns

## Git Workflow

### Branch Strategy
- `main`: Production-ready code
- `develop`: Development branch
- `feature/*`: Feature branches
- `hotfix/*`: Critical fixes

### Commit Messages
```
feat: add medication reminder functionality
fix: resolve notification scheduling issue
docs: update setup instructions
test: add unit tests for reminder dao
```

## IDE Configuration

### Recommended Plugins
- **Kotlin**: Language support
- **Android**: Official Android plugin
- **GitToolBox**: Enhanced Git integration

### Code Formatting
1. Go to **Settings** → **Editor** → **Code Style** → **Kotlin**
2. Import `kotlin-style-guide.xml` if available
3. Enable "Reformat code" on save

## Troubleshooting

### Common Issues

#### Gradle Sync Failed
```bash
# Clear Gradle cache
./gradlew clean --refresh-dependencies

# Reset Gradle wrapper
gradle wrapper --gradle-version 8.1.1
```

#### Build Failures
1. Check SDK version compatibility
2. Verify all dependencies are available
3. Clear and rebuild project

#### Emulator Issues
1. Ensure Hardware Acceleration is enabled
2. Allocate sufficient RAM to emulator
3. Use system images compatible with your OS

### Getting Help
- Check Android Studio's **Event Log** for errors
- Use **Build** → **Clean Project** for build issues
- Restart Android Studio if needed

## Contributing Guidelines

### Before Submitting
1. Run all tests and ensure they pass
2. Follow code style guidelines
3. Update documentation if needed
4. Add appropriate comments

### Pull Request Process
1. Create feature branch from `develop`
2. Make changes and commit
3. Push branch and create PR
4. Wait for review and approval

## Resources

### Documentation
- [Android Developer Guide](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)

### Tools
- [Android Studio](https://developer.android.com/studio)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Language](https://kotlinlang.org/)

### Community
- [Android Developers Reddit](https://www.reddit.com/r/androiddev/)
- [Kotlin Slack](https://kotlinlang.slack.com/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/android)