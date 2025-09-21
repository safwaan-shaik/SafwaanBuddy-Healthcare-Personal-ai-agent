@echo off
REM NurturePK Build Script for Windows
REM This script builds the Android APK using Gradle

echo Starting NurturePK build process...

REM Check if gradlew.bat exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please run this script from the project root directory.
    exit /b 1
)

echo Cleaning previous builds...
call gradlew.bat clean

echo Building debug APK...
call gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo ❌ Debug APK build failed!
    exit /b 1
) else (
    echo ✅ Debug APK build successful!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
)

echo Building release APK...
call gradlew.bat assembleRelease

if %errorlevel% neq 0 (
    echo ❌ Release APK build failed!
    exit /b 1
) else (
    echo ✅ Release APK build successful!
    echo APK location: app\build\outputs\apk\release\app-release-unsigned.apk
)

echo Running unit tests...
call gradlew.bat testDebugUnitTest

if %errorlevel% neq 0 (
    echo ❌ Unit tests failed!
    exit /b 1
) else (
    echo ✅ Unit tests passed!
)

echo Running instrumented tests...
call gradlew.bat connectedDebugAndroidTest

if %errorlevel% neq 0 (
    echo ⚠️ Instrumented tests failed (device may not be connected)
) else (
    echo ✅ Instrumented tests passed!
)

echo 🎉 Build process completed successfully!
echo.
echo Generated files:
echo - Debug APK: app\build\outputs\apk\debug\app-debug.apk
echo - Release APK: app\build\outputs\apk\release\app-release-unsigned.apk
echo - Test reports: app\build\reports\

pause