#!/usr/bin/env python3
"""
J.A.R.V.I.S. Non-Admin Compatibility Checker
This script checks if the system can run without administrator privileges
"""

import os
import sys
import subprocess
import importlib
import tempfile
from pathlib import Path

def check_python_version():
    """Check if Python version is compatible"""
    try:
        if sys.version_info < (3, 8):
            print("❌ Python 3.8 or higher required")
            return False
        print(f"✅ Python {sys.version_info.major}.{sys.version_info.minor} detected")
        return True
    except Exception as e:
        print(f"❌ Python check failed: {e}")
        print("Please install Python 3.8+ from https://python.org")
        return False

def check_write_permissions():
    """Check if we can write to necessary directories"""
    test_dirs = ['Data', 'Healthcare/Database', 'Healthcare/Logs']
    
    for dir_path in test_dirs:
        try:
            Path(dir_path).mkdir(parents=True, exist_ok=True)
            test_file = Path(dir_path) / 'test_write.tmp'
            test_file.write_text('test')
            test_file.unlink()
            print(f"✅ Write access to {dir_path}")
        except PermissionError:
            print(f"❌ No write access to {dir_path}")
            return False
        except Exception as e:
            print(f"⚠️ Warning: Issue with {dir_path}: {e}")
    
    return True

def check_required_modules():
    """Check if required modules can be imported"""
    required_modules = [
        'dotenv', 'PyQt5', 'requests', 'speech_recognition', 
        'pyttsx3', 'keyboard', 'pyautogui', 'groq', 'cohere'
    ]
    
    optional_modules = [
        'pywhatkit', 'BeautifulSoup4', 'cryptography', 'sqlalchemy'
    ]
    
    missing_required = []
    missing_optional = []
    
    for module in required_modules:
        try:
            importlib.import_module(module.replace('BeautifulSoup4', 'bs4').replace('PyQt5', 'PyQt5.QtCore'))
            print(f"✅ {module} available")
        except ImportError:
            print(f"❌ {module} missing")
            missing_required.append(module)
    
    for module in optional_modules:
        try:
            importlib.import_module(module.replace('BeautifulSoup4', 'bs4'))
            print(f"✅ {module} available")
        except ImportError:
            print(f"⚠️ {module} missing (optional)")
            missing_optional.append(module)
    
    return len(missing_required) == 0, missing_required, missing_optional

def check_audio_access():
    """Check if audio systems are accessible without admin"""
    try:
        import pyaudio
        print("✅ PyAudio available for microphone access")
        audio_ok = True
    except ImportError:
        print("⚠️ PyAudio not available - using alternative speech recognition")
        audio_ok = False
    
    try:
        import pyttsx3
        engine = pyttsx3.init()
        print("✅ Text-to-speech engine available")
        engine.stop()
        del engine
    except Exception as e:
        print(f"⚠️ Text-to-speech issue: {e}")
        audio_ok = False
    
    return audio_ok

def check_system_operations():
    """Check if system operations work without admin"""
    try:
        # Test keyboard shortcuts (should work without admin)
        import keyboard
        print("✅ Keyboard control available")
    except ImportError:
        print("❌ Keyboard control not available")
        return False
    
    try:
        # Test screenshot capability
        import pyautogui
        print("✅ Screenshot capability available")
    except ImportError:
        print("❌ Screenshot capability not available")
        return False
    
    return True

def check_network_access():
    """Check if network requests work"""
    try:
        import requests
        response = requests.get('https://httpbin.org/get', timeout=5)
        if response.status_code == 200:
            print("✅ Network access working")
            return True
    except Exception as e:
        print(f"⚠️ Network issue: {e}")
        return False

def main():
    """Run all compatibility checks"""
    print("="*60)
    print("🤖 J.A.R.V.I.S. Non-Admin Compatibility Check")
    print("="*60)
    print()
    
    checks = [
        ("Python Version", check_python_version),
        ("Write Permissions", check_write_permissions),
        ("Required Modules", lambda: check_required_modules()[0]),
        ("Audio Access", check_audio_access),
        ("System Operations", check_system_operations),
        ("Network Access", check_network_access)
    ]
    
    results = []
    for name, check_func in checks:
        print(f"\n🔍 Checking {name}...")
        try:
            result = check_func()
            results.append((name, result))
        except Exception as e:
            print(f"❌ Error checking {name}: {e}")
            results.append((name, False))
    
    print("\n" + "="*60)
    print("📊 COMPATIBILITY SUMMARY")
    print("="*60)
    
    passed = 0
    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{name}: {status}")
        if result:
            passed += 1
    
    print(f"\nPassed: {passed}/{len(results)} checks")
    
    if passed == len(results):
        print("\n🎉 J.A.R.V.I.S. is ready to run without admin privileges!")
        print("You can start the system using: python Main.py")
    elif passed >= len(results) * 0.8:  # 80% pass rate
        print("\n⚠️ J.A.R.V.I.S. should work with some limitations")
        print("Some advanced features may not be available")
    else:
        print("\n❌ Significant issues detected")
        print("Please install missing dependencies or check permissions")
    
    # Check for missing modules and provide installation commands
    _, missing_required, missing_optional = check_required_modules()
    if missing_required:
        print(f"\n📦 Install missing required modules:")
        print(f"pip install {' '.join(missing_required)}")
    
    if missing_optional:
        print(f"\n📦 Optional modules to install:")
        print(f"pip install {' '.join(missing_optional)}")

if __name__ == "__main__":
    main()