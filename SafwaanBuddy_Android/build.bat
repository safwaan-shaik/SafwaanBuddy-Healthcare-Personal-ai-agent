@echo off
echo Checking for Java installation...
echo.

REM Try to find Java in common locations
set JAVA_FOUND=0

REM Check if JAVA_HOME is already set
if defined JAVA_HOME (
    echo JAVA_HOME is already set to: %JAVA_HOME%
    if exist "%JAVA_HOME%\bin\java.exe" (
        set JAVA_FOUND=1
        echo Found Java at JAVA_HOME
    )
)

REM If JAVA_HOME not set or invalid, try to find Java in common locations
if %JAVA_FOUND% equ 0 (
    echo JAVA_HOME not set or invalid. Searching for Java...
    
    REM Check Program Files\Java
    if exist "C:\Program Files\Java" (
        for /d %%i in ("C:\Program Files\Java\*") do (
            if exist "%%i\bin\java.exe" (
                set JAVA_HOME=%%i
                set JAVA_FOUND=1
                echo Found Java at: %%i
                goto javaFound
            )
        )
    )
    
    REM Check Android Studio JBR (newer versions)
    if exist "C:\Program Files\Android\Android Studio\jbr" (
        if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
            set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
            set JAVA_FOUND=1
            echo Found Java at: C:\Program Files\Android\Android Studio\jbr
            goto javaFound
        )
    )
    
    REM Check older Android Studio location
    if exist "C:\Program Files\Android\Android Studio\jre" (
        if exist "C:\Program Files\Android\Android Studio\jre\bin\java.exe" (
            set JAVA_HOME=C:\Program Files\Android\Android Studio\jre
            set JAVA_FOUND=1
            echo Found Java at: C:\Program Files\Android\Android Studio\jre
            goto javaFound
        )
    )
)

:javaFound
if %JAVA_FOUND% equ 0 (
    echo ERROR: Java not found!
    echo Please install Java JDK 8 or higher and set JAVA_HOME environment variable.
    echo You can download Java from: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM Add Java to PATH
set PATH=%JAVA_HOME%\bin;%PATH%

echo.
echo Verifying Java...
java -version
echo.

echo Building Healthcare App...
echo.

REM Use gradlew.bat from the project directory
call gradlew.bat clean assembleDebug

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ BUILD SUCCESSFUL!
    echo APK: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ❌ BUILD FAILED!
    echo Please check the error messages above.
    echo Make sure you have Android SDK installed and ANDROID_HOME environment variable set.
)

echo.
pause