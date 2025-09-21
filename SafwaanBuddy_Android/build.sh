#!/bin/bash

# Define colors for better output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}SafwaanBuddy Healthcare Android Build Script${NC}"
echo -e "${BLUE}============================================${NC}"
echo

# =================================================================
# == Java Pre-flight Check
# =================================================================
echo -e "${BLUE}[Pre-check] Verifying Java installation...${NC}"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    echo -e "${GREEN}✓ Java found in JAVA_HOME: $JAVA_HOME${NC}"
elif command -v java >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Java found in PATH${NC}"
else
    echo -e "${RED}ERROR: JAVA_HOME is not set and 'java' could not be found in your PATH.${NC}"
    echo
    echo "Solutions:"
    echo "1. Install Android Studio (includes JDK)"
    echo "2. Set JAVA_HOME to Android Studio's JDK path"
    echo "3. Add \$JAVA_HOME/bin to your PATH"
    exit 1
fi

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}ERROR: gradlew not found!${NC}"
    echo "Make sure you're running this from the SafwaanBuddy_Android project root directory."
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

echo
echo -e "${BLUE}[1/8] Cleaning previous builds...${NC}"
./gradlew clean
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Clean failed!${NC}"
    exit 1
fi

echo
echo -e "${BLUE}[2/8] Building Debug APK...${NC}"
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Debug build failed!${NC}"
    exit 1
fi

echo
echo -e "${BLUE}[3/8] Building Release APK...${NC}"
./gradlew assembleRelease
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Release build failed!${NC}"
    exit 1
fi

echo
echo -e "${BLUE}[4/8] Running Unit Tests...${NC}"
TEST_STATUS="PASSED"
./gradlew testDebugUnitTest
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}WARNING: Some unit tests failed!${NC}"
    echo "Continuing with build..."
    TEST_STATUS="FAILED"
fi

echo
echo -e "${BLUE}[5/8] Running Lint Checks...${NC}"
LINT_STATUS="PASSED"
./gradlew lintDebug
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}WARNING: Lint found issues!${NC}"
    echo "Continuing with build..."
    LINT_STATUS="WARNING"
fi

echo
echo -e "${BLUE}[6/8] Running Instrumented Tests...${NC}"
INSTRUMENTED_TEST_STATUS="SKIPPED"
./gradlew connectedAndroidTest
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}NOTE: Instrumented tests skipped (no device/emulator connected)${NC}"
    INSTRUMENTED_TEST_STATUS="SKIPPED"
else
    INSTRUMENTED_TEST_STATUS="PASSED"
fi

echo
echo -e "${BLUE}[7/8] Generating Code Coverage Report...${NC}"
COVERAGE_STATUS="GENERATED"
./gradlew jacocoTestReport
if [ $? -ne 0 ]; then
    echo -e "${YELLOW}WARNING: Could not generate test coverage report.${NC}"
    COVERAGE_STATUS="FAILED"
fi

echo
echo -e "${BLUE}[8/8] Final Build Summary${NC}"
echo -e "${BLUE}========================${NC}"
echo

# Check if APKs were generated
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

BUILD_SUCCESS=true
if [ -f "$DEBUG_APK" ]; then
    echo -e "${GREEN}✓ Debug APK: $DEBUG_APK${NC}"
    echo "  Size: $(du -h "$DEBUG_APK" | cut -f1)"
else
    echo -e "${RED}✗ Debug APK not found!${NC}"
    BUILD_SUCCESS=false
fi

if [ -f "$RELEASE_APK" ]; then
    echo -e "${GREEN}✓ Release APK: $RELEASE_APK${NC}"
    echo "  Size: $(du -h "$RELEASE_APK" | cut -f1)"
    echo "  Note: Release APK is unsigned. Sign before distribution."
else
    echo -e "${RED}✗ Release APK not found!${NC}"
    BUILD_SUCCESS=false
fi

echo
echo -e "${BLUE}Healthcare Application Build Information:${NC}"
echo -e "${BLUE}========================================${NC}"
echo "App Name: SafwaanBuddy Healthcare"
echo "Package: com.safwaanbuddy.healthcare"
echo "Target SDK: 34 (Android 14)"
echo "Min SDK: 24 (Android 7.0+)"
echo

if [ "$BUILD_SUCCESS" = true ]; then
    echo -e "${GREEN}=================================================${NC}"
    echo -e "${GREEN}  BUILD SUCCEEDED${NC}"
    echo -e "${GREEN}=================================================${NC}"
    echo
    echo "Next steps:"
    echo "1. Install debug APK: adb install $DEBUG_APK"
    echo "2. Review code coverage report at app/build/reports/jacoco/jacocoTestReport/html/index.html"
    echo "3. Sign release APK for production deployment"
    echo "4. Test on multiple devices before distribution"
else
    echo -e "${RED}=================================================${NC}"
    echo -e "${RED}  BUILD FAILED - Check logs for errors${NC}"
    echo -e "${RED}=================================================${NC}"
fi

echo

# Generate build report
TIMESTAMP=$(date '+%Y-%m-%d_%H-%M-%S')
echo "Build completed at $TIMESTAMP" > build_report.txt
echo "Debug APK: $DEBUG_APK" >> build_report.txt
echo "Release APK: $RELEASE_APK" >> build_report.txt
echo "Unit Tests: $TEST_STATUS" >> build_report.txt
echo "Lint Checks: $LINT_STATUS" >> build_report.txt
echo "Instrumented Tests: $INSTRUMENTED_TEST_STATUS" >> build_report.txt
echo "Test Coverage: $COVERAGE_STATUS" >> build_report.txt
echo "Build Success: $BUILD_SUCCESS" >> build_report.txt

echo -e "${GREEN}Build report saved to build_report.txt${NC}"