#!/bin/bash

# NurturePK Build Script for Linux/Mac
# This script builds the Android APK using Gradle

echo "Starting NurturePK build process..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found. Please run this script from the project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

echo "Cleaning previous builds..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Debug APK build successful!"
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Debug APK build failed!"
    exit 1
fi

echo "Building release APK..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    echo "✅ Release APK build successful!"
    echo "APK location: app/build/outputs/apk/release/app-release-unsigned.apk"
else
    echo "❌ Release APK build failed!"
    exit 1
fi

echo "Running unit tests..."
./gradlew testDebugUnitTest

if [ $? -eq 0 ]; then
    echo "✅ Unit tests passed!"
else
    echo "❌ Unit tests failed!"
    exit 1
fi

echo "Running instrumented tests..."
./gradlew connectedDebugAndroidTest

if [ $? -eq 0 ]; then
    echo "✅ Instrumented tests passed!"
else
    echo "⚠️ Instrumented tests failed (device may not be connected)"
fi

echo "🎉 Build process completed successfully!"
echo ""
echo "Generated files:"
echo "- Debug APK: app/build/outputs/apk/debug/app-debug.apk"
echo "- Release APK: app/build/outputs/apk/release/app-release-unsigned.apk"
echo "- Test reports: app/build/reports/"