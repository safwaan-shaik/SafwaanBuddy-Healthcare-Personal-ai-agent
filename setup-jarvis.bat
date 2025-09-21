@echo off
echo ===============================================
echo        J.A.R.V.I.S Setup Script
echo        No Admin Access Required
echo ===============================================
echo.
echo Step 1: Checking Python installation...
python --version
if %errorlevel% neq 0 (
    echo ERROR: Python is not installed or not in PATH!
    echo Please install Python from: https://python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation
    pause
    exit /b 1
)

echo.
echo Step 2: Creating virtual environment...
python -m venv venv
if %errorlevel% neq 0 (
    echo ERROR: Failed to create virtual environment
    pause
    exit /b 1
)

echo.
echo Step 3: Activating virtual environment...
call venv\Scripts\activate.bat

echo.
echo Step 4: Upgrading pip...
python -m pip install --upgrade pip

echo.
echo Step 5: Installing required packages...
pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo ERROR: Failed to install some packages
    echo This might be due to missing system dependencies
    echo Some packages like PyQt5 might need Visual C++ Redistributable
    pause
)

echo.
echo ===============================================
echo Setup completed!
echo ===============================================
echo.
echo Next steps:
echo 1. Edit .env file with your API keys
echo 2. Run start-jarvis.bat to start the agent
echo.
pause