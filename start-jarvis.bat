@echo off
echo Starting J.A.R.V.I.S Personal AI Agent...
echo.

REM Check if virtual environment exists
if not exist "venv\Scripts\activate.bat" (
    echo Virtual environment not found!
    echo Please run setup-jarvis.bat first
    pause
    exit /b 1
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Check if .env file exists
if not exist ".env" (
    echo .env file not found!
    echo Please configure your API keys in .env file
    pause
    exit /b 1
)

REM Start Jarvis
echo Current directory: %CD%
echo Starting Jarvis...
python Main.py

pause