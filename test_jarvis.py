#!/usr/bin/env python3

"""
Test script to verify Jarvis core functionality
"""

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

def test_imports():
    """Test if all core modules can be imported"""
    print("Testing imports...")
    
    try:
        from Backend.Model import FirstLayerDMM
        print("✅ Backend.Model imported successfully")
    except Exception as e:
        print(f"❌ Backend.Model import failed: {e}")
        return False
    
    try:
        from Backend.Chatbot import ChatBot
        print("✅ Backend.Chatbot imported successfully")
    except Exception as e:
        print(f"❌ Backend.Chatbot import failed: {e}")
        return False
    
    try:
        from Backend.SimpleSpeechToText import SpeechRecognition
        print("✅ Backend.SimpleSpeechToText imported successfully")
    except Exception as e:
        print(f"❌ Backend.SimpleSpeechToText import failed: {e}")
        return False
    
    return True

def test_decision_making():
    """Test the decision making model"""
    print("\nTesting decision making...")
    
    try:
        from Backend.Model import FirstLayerDMM
        result = FirstLayerDMM("Hello, how are you?")
        print(f"✅ Decision making test result: {result}")
        return True
    except Exception as e:
        print(f"❌ Decision making test failed: {e}")
        return False

def test_speech_recognition():
    """Test speech recognition"""
    print("\nTesting speech recognition...")
    
    try:
        from Backend.SimpleSpeechToText import SpeechRecognition
        # This should return test input without requiring actual speech
        result = SpeechRecognition()
        print(f"✅ Speech recognition test result: {result}")
        return True
    except Exception as e:
        print(f"❌ Speech recognition test failed: {e}")
        return False

def main():
    """Run all tests"""
    print("🤖 Testing Jarvis Core Functionality\n")
    print("=" * 50)
    
    tests_passed = 0
    total_tests = 3
    
    if test_imports():
        tests_passed += 1
    
    if test_decision_making():
        tests_passed += 1
    
    if test_speech_recognition():
        tests_passed += 1
    
    print("\n" + "=" * 50)
    print(f"Tests Results: {tests_passed}/{total_tests} passed")
    
    if tests_passed == total_tests:
        print("🎉 All tests passed! Jarvis core functionality is working!")
        return True
    else:
        print("❌ Some tests failed. Check the errors above.")
        return False

if __name__ == "__main__":
    main()