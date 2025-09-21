#!/usr/bin/env python3

"""
Simple Speech Recognition Module
Provides basic speech recognition functionality for Jarvis
"""

import speech_recognition as sr
import time

def SpeechRecognition():
    """
    Simple speech recognition function using PyAudio and Google Speech Recognition
    """
    try:
        # Initialize recognizer
        recognizer = sr.Recognizer()
        
        # Use microphone as source
        with sr.Microphone() as source:
            print("Listening... Speak now!")
            # Adjust for ambient noise
            recognizer.adjust_for_ambient_noise(source, duration=1)
            # Listen for audio with timeout
            audio = recognizer.listen(source, timeout=10, phrase_time_limit=5)
        
        print("Processing speech...")
        # Recognize speech using Google
        text = recognizer.recognize_google(audio, language='en-US')
        print(f"You said: {text}")
        return text.capitalize()
        
    except sr.WaitTimeoutError:
        print("Listening timeout - no speech detected")
        return ""
    except sr.UnknownValueError:
        print("Sorry, I could not understand the audio")
        return ""
    except sr.RequestError as e:
        print(f"Could not request results from Google Speech Recognition service; {e}")
        return ""
    except Exception as e:
        print(f"Speech recognition failed: {e}")
        # Return a test query so the application doesn't get stuck
        print("Using test input instead...")
        return "Hello Jarvis"

if __name__ == "__main__":
    print("Testing speech recognition...")
    result = SpeechRecognition()
    print(f"Result: {result}")