@echo off
echo ===================================================
echo    J.A.R.V.I.S. - Personal AI Assistant
echo    Starting without Administrator privileges
echo ===================================================
echo.

REM Check if Python is available
echo Checking for Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Python is not installed or not in PATH
    echo.
    echo Please install Python 3.8+ from: https://python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation
    echo.
    echo Alternative: Try using 'py' command if Python Launcher is installed
    echo Example: py Main.py
    echo.
    pause
    exit /b 1
)

echo Python found. Checking dependencies...

REM Create virtual environment if it doesn't exist
if not exist "venv" (
    echo Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Install/update requirements
echo Installing dependencies...
pip install -r requirements.txt --user --no-warn-script-location

REM Create necessary directories
if not exist "Data" mkdir Data
if not exist "Healthcare\Database" mkdir Healthcare\Database
if not exist "Healthcare\Logs" mkdir Healthcare\Logs

echo.
echo ===================================================
echo    Starting J.A.R.V.I.S. (Non-Admin Mode)
echo ===================================================
echo.
echo Note: Running without administrator privileges
echo Some advanced system functions may be limited
echo.

REM Start the application
python Main.py

pause