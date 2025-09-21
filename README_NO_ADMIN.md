# J.A.R.V.I.S. - Non-Administrator Setup Guide

## 🚀 Quick Start (No Admin Required)

### Option 1: Using the Batch Script
1. Double-click `start_jarvis_no_admin.bat`
2. The script will automatically set up and start J.A.R.V.I.S.

### Option 2: Manual Setup
1. Check compatibility: `python check_compatibility.py`
2. Install dependencies: `pip install -r requirements.txt --user`
3. Start J.A.R.V.I.S.: `python Main.py`

## 🔧 Non-Admin Limitations & Workarounds

### System Operations
- **Shutdown Command**: Uses 60-second delay instead of immediate shutdown
  - Allows time to cancel with `shutdown /a` if needed
  - Voice command: "shut down computer"

- **Audio Control**: Falls back to keyboard shortcuts if advanced audio control unavailable
  - Volume up/down: Uses Windows keyboard shortcuts
  - Mute/unmute: Uses Windows toggle shortcut

### File Access
- All data stored in local `Data/` directory
- Healthcare database stored in `Healthcare/Database/`
- No system-wide registry modifications
- No Program Files access required

### Enhanced Features Available
✅ **Weather Information** - Get current weather for any city
✅ **News Headlines** - Latest news from multiple sources  
✅ **Stock Prices** - Real-time stock market data
✅ **Wikipedia Search** - Knowledge lookup and summaries
✅ **Location Finder** - Geographic location information
✅ **Email Integration** - Send emails via SMTP
✅ **Joke Telling** - Entertainment and humor
✅ **Cricket Scores** - Live cricket match information

### Healthcare Features Available
✅ **Medication Reminders** - Smart scheduling system
✅ **Symptom Logging** - Track health indicators
✅ **Lab Results Analysis** - Medical document OCR
✅ **Pregnancy Care** - Specialized prenatal support
✅ **Emergency Alerts** - Health emergency detection
✅ **Secure Database** - Encrypted health data storage

## 🎯 Voice Commands

### Basic Commands
- "Hello Jarvis" - Wake up
- "What's the weather?" - Get weather info
- "Tell me the news" - Get headlines
- "Play [song name]" - Play on YouTube
- "Open [app name]" - Launch applications
- "Take a screenshot" - Capture screen

### Healthcare Commands
- "Medication reminder" - Set med reminders
- "Log symptom [symptom]" - Record symptoms
- "Health emergency" - Emergency protocols
- "Prenatal care" - Pregnancy support

### Enhanced Features
- "What's the stock price of [company]?" - Stock info
- "Tell me about [topic]" - Wikipedia search
- "Where is [location]?" - Location finder
- "Tell me a joke" - Entertainment
- "Cricket score" - Live cricket updates

## 🔑 Required API Keys

Add these to your `.env` file for full functionality:

```
# Core AI Services
COHERE_API_KEY=your_cohere_key
GroqAPIKey=your_groq_key
OPENAI_API_KEY=your_openai_key

# Enhanced Features
OPENWEATHER_API_KEY=your_weather_key
NEWS_API_KEY=your_news_key
ALPHA_VANTAGE_API_KEY=your_stock_key
OPENCAGE_API_KEY=your_geocoding_key
CRICKET_API_KEY=your_cricket_key

# User Configuration
Username=YourName
Assistantname=Jarvis

# Email Configuration (Optional)
EMAIL_ADDRESS=your_email@example.com
EMAIL_PASSWORD=your_app_password
SMTP_SERVER=smtp.gmail.com
SMTP_PORT=587
```

## 🛠️ Troubleshooting

### "Permission Denied" Errors
- Run: `python check_compatibility.py` to diagnose
- Ensure all files are in user directory (not Program Files)
- Try: `pip install --user [package]` for any missing packages

### Audio Issues
- Install PyAudio: `pip install pyaudio --user`
- Windows: May need Microsoft Visual C++ Build Tools
- Alternative: System will use keyboard shortcuts for audio control

### Module Import Errors
- Update pip: `python -m pip install --upgrade pip --user`
- Install requirements: `pip install -r requirements.txt --user`
- Check Python version: Must be 3.8+

### Network/API Issues
- Check internet connection
- Verify API keys in `.env` file
- Some features work with demo data if APIs unavailable

## 📋 System Requirements

- **Python**: 3.8 or higher
- **OS**: Windows 10/11 (no admin rights needed)
- **RAM**: 4GB minimum, 8GB recommended
- **Disk**: 2GB free space for dependencies
- **Network**: Internet for API features

## 🔒 Security Notes

- All health data encrypted locally
- No admin privileges required
- User-level file access only
- API keys stored in local .env file
- No system registry modifications

## 📞 Support

If you encounter issues:
1. Run the compatibility checker
2. Check the troubleshooting section
3. Ensure all API keys are configured
4. Verify Python and pip are updated

J.A.R.V.I.S. is designed to work seamlessly without administrator privileges while maintaining full functionality for healthcare monitoring, AI assistance, and enhanced features.