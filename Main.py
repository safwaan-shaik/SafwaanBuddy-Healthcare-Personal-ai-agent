from Frontend.GUI import (GraphicalUserInerface, SetAssistantStatus,
                          ShowTextToScreen, TempDirectoryPath, 
                          SetMicrophoneStatus, AnswerModifier,
                          QueryModifier, GetAssistantStatus, GetMicrophoneStatus)

from Backend.Model import FirstLayerDMM
from Backend.RealtimeSearchEngine import RealtimeSearchEngine
from Backend.Automation import Automation
from Backend.Chatbot import ChatBot
from Backend.TextToSpeech import TextToSpeech
# Try to import SpeechRecognition, fallback to simple version if SSL issues
try:
    from Backend.SpeechToText import SpeechRecognition
except Exception as e:
    print(f"Warning: Could not import SpeechToText due to SSL issues: {e}")
    print("Using simple speech recognition fallback...")
    from Backend.SimpleSpeechToText import SpeechRecognition

# Healthcare Module Integration
try:
    from Healthcare.Core.pregnancy_care import pregnancy_care
    from Healthcare.Database.models import HealthcareDatabase
    from Healthcare.Core.medication_scheduler import initialize_medication_system
    
    # Initialize healthcare system
    healthcare_db = HealthcareDatabase()
    medication_scheduler, voice_medication_interface = initialize_medication_system(healthcare_db)
    
    print("✅ J.A.R.V.I.S. Healthcare Module loaded successfully")
    HEALTHCARE_ENABLED = True
except Exception as e:
    print(f"⚠️ Warning: Healthcare module could not be loaded: {e}")
    print("J.A.R.V.I.S. will run without healthcare features.")
    HEALTHCARE_ENABLED = False

# Enhanced Features Integration
try:
    from Backend.EnhancedFeatures import (
        WeatherService, NewsService, StockService, EmailService, 
        JokeService, WikipediaService, LocationService, CricketService
    )
    from Backend.Automation import HandleEnhancedFeatures
    
    print("✅ J.A.R.V.I.S. Enhanced Features loaded successfully")
    ENHANCED_FEATURES_ENABLED = True
except Exception as e:
    print(f"⚠️ Warning: Enhanced features could not be loaded: {e}")
    print("J.A.R.V.I.S. will run without enhanced features.")
    ENHANCED_FEATURES_ENABLED = False
#----------------------------------------------------------------------------------
# Other Requirements
from dotenv import dotenv_values
from asyncio import run
from time import sleep
import subprocess
import threading
import json
import os

env_vars = dotenv_values('.env')
Username = env_vars.get('Username')
Assistantname = env_vars.get('Assistantname')
DefaultMessage = f'''
{Username} : Hello {Assistantname}, How are You.
{Assistantname} : Hi {Username}, I am doing Good, So what's your first command?'''

subprocesses = list()
functions = ['open', 'close', 'play', 'system', 'content', 'google search', 'youtube search']#keywords.copy()

# Healthcare functions
healthcare_functions = [
    'medication reminder', 'take medication', 'log symptom', 'check lab results', 
    'emergency alert', 'prenatal appointment', 'upload prescription', 'medication schedule', 
    'health summary', 'health emergency', 'prenatal care', 'pregnancy care', 'medication', 
    'health check', 'doctor appointment', 'call doctor', 'contraction timer'
]

# Enhanced features functions
enhanced_functions = [
    'weather', 'news', 'headlines', 'stock price', 'share price', 'tell joke', 'joke',
    'wikipedia', 'tell me about', 'find location', 'where is', 'location of', 
    'cricket score', 'cricket', 'send email', 'email', 'current temperature'
]

def ShowDefaultChat(): # This is Helpful when the ChatLog.json is Empty and New Chat is started.
    file = open(r'Data\ChatLog.json', 'r', encoding='utf-8')
    if len(file.read())<5:
        with open(TempDirectoryPath('Database.data'), 'w', encoding='utf-8') as file:
            file.write("")
        with open(TempDirectoryPath('Database.data'), 'w', encoding='utf-8') as file:
            file.write(DefaultMessage)
    file.close()
def ReadChatLogJson():
    with open(r'Data\ChatLog.json', 'r', encoding='utf-8') as file:
        chat_data = json.load(file)
    return chat_data

def ChatLogIntegration():
    json_data = ReadChatLogJson()
    formatted_chatlog = ""
    for entry in json_data:
        if entry['role'] == 'user':
            formatted_chatlog += f"User: {entry['content']}\n"
        elif entry['role'] == 'assistant':
            formatted_chatlog += f"Assistant: {entry['content']}\n"
    
    formatted_chatlog = formatted_chatlog.replace("User", Username+" ")
    formatted_chatlog = formatted_chatlog.replace("Assistant", Assistantname+" ")

    with open(TempDirectoryPath("Database.data"), 'w', encoding='utf-8') as file:
        file.write(AnswerModifier(formatted_chatlog))

def ShowChatsOnGUI():
    file = open(TempDirectoryPath("Database.data"), 'r', encoding='utf-8')
    data = file.read()
    if len(str(data)) > 0:
        lines = data.split('\n')
        result = '\n'.join(lines)
        file.close()
        file = open(TempDirectoryPath('Responses.data'), 'w', encoding='utf-8')
        file.write(result); file.close()

def InitialExecution():
    SetMicrophoneStatus('False')
    ShowTextToScreen("")
    ShowDefaultChat()
    ChatLogIntegration()
    ShowChatsOnGUI()


InitialExecution()

def process_healthcare_command(command_query: str, original_query: str) -> str:
    """
    Process healthcare voice commands
    """
    if not HEALTHCARE_ENABLED:
        return "Healthcare features are not available at the moment."
    
    try:
        # Parse command and extract details
        command_parts = command_query.strip().split(' ', 1)
        command = command_parts[0].lower()
        details = command_parts[1] if len(command_parts) > 1 else ""
        
        print(f"Processing healthcare command: {command} with details: {details}")
        
        # Map voice commands to healthcare module functions
        if command == "medication":
            if "reminder" in details or "remind" in original_query.lower():
                return pregnancy_care.process_healthcare_command("medication_reminder", details)
            elif "took" in details or "taken" in original_query.lower():
                return pregnancy_care.process_healthcare_command("take_medication", details)
            else:
                return voice_medication_interface.process_medication_voice_command(original_query, details)
        
        elif command in ["upload", "prescription"]:
            return pregnancy_care.process_healthcare_command("upload_prescription", details)
        
        elif command in ["log", "symptom"]:
            return pregnancy_care.process_healthcare_command("log_symptom", details)
        
        elif command in ["check", "lab"]:
            return pregnancy_care.process_healthcare_command("check_lab_results", details)
        
        elif command in ["health", "emergency"]:
            return pregnancy_care.process_healthcare_command("health_emergency", details)
        
        elif command in ["prenatal", "pregnancy"]:
            if "care" in details:
                return pregnancy_care.process_healthcare_command("prenatal_care", details)
            else:
                return pregnancy_care.process_healthcare_command("pregnancy_care", details)
        
        elif command in ["contraction", "timer"]:
            return pregnancy_care.process_healthcare_command("contraction_timer", details)
        
        elif command in ["call", "doctor"]:
            return pregnancy_care.process_healthcare_command("call_doctor", details)
        
        else:
            # Try general healthcare processing
            return pregnancy_care.process_healthcare_command(command, details)
            
    except Exception as e:
        print(f"Error processing healthcare command: {e}")
        return "I had trouble processing that healthcare request. Please try again."

def MainExecution():
    TaskExecution = False
    SetAssistantStatus("Ready to Perform...")
    try:
        Query = SpeechRecognition()
        ShowTextToScreen(f"{Username} : {Query}")
        if not Query or not Query.strip():
            ShowTextToScreen(f"{Assistantname} : Sorry, I didn't catch that. Please try again.")
            return False
        SetAssistantStatus("Thinking...")
        Decision = FirstLayerDMM(Query)

        print("")
        print(f"Decision : {Decision}")
        print("")

        G = any([i for i in Decision if i.startswith("general")])
        R = any([i for i in Decision if i.startswith("realtime")])

        mearged_query = " and ".join(
            [" ".join(i.split()[1:]) for i in Decision if i.startswith('general') or i.startswith('realtime')]
        )

        for queries in Decision:
            if TaskExecution == False:
                if any(queries.startswith(func) for func in functions):
                    run(Automation(list(Decision)))
                    TaskExecution = True
                    
                # Healthcare command processing
                elif HEALTHCARE_ENABLED and any(queries.startswith(func) for func in healthcare_functions):
                    Answer = process_healthcare_command(queries, Query)
                    ShowTextToScreen(f"{Assistantname} : {Answer}")
                    SetAssistantStatus("...Answering...")
                    TextToSpeech(Answer)
                    TaskExecution = True
                    return True
                    
                # Enhanced features command processing
                elif ENHANCED_FEATURES_ENABLED and any(queries.startswith(func) for func in enhanced_functions):
                    Answer = HandleEnhancedFeatures(Query)
                    ShowTextToScreen(f"{Assistantname} : {Answer}")
                    SetAssistantStatus("...Answering...")
                    TextToSpeech(Answer)
                    TaskExecution = True
                    return True
            
        if G and R or R:
            SetAssistantStatus("Searching...")
            Answer = RealtimeSearchEngine(QueryModifier(mearged_query))
            ShowTextToScreen(f"{Assistantname} : {Answer}")
            SetAssistantStatus("...Answering...")
            TextToSpeech(Answer)
            return True

        else:
            for Queries in Decision:
                if 'general' in Queries:
                    SetAssistantStatus("Thinking...")
                    QueryFinal = Queries.replace("general ", "")
                    Answer = ChatBot(QueryModifier(QueryFinal))
                    ShowTextToScreen(f'{Assistantname} : {Answer}')
                    SetAssistantStatus("Answering...")
                    TextToSpeech(Answer)
                    return True
                
                elif 'realtime' in Queries:
                    SetAssistantStatus("Searching...")
                    QueryFinal = Queries.replace("realtime ", "")
                    Answer = ChatBot(QueryModifier(QueryFinal))
                    ShowTextToScreen(f'{Assistantname} : {Answer}')
                    SetAssistantStatus("Answering...")
                    TextToSpeech(Answer)
                    return True
                
                elif ('terminate' in Queries) or ('exit' in Queries):
                    QueryFinal = "Okay, GoodBye!!"
                    Answer = ChatBot(QueryModifier(QueryFinal))
                    ShowTextToScreen(f"{Assistantname} : {Answer}")
                    SetAssistantStatus("Executing...")
                    TextToSpeech(Answer)
                    os._exit(1)
    finally:
        SetAssistantStatus("Ready to Perform...")
        SetMicrophoneStatus("True")

def FirstThread():
    while True:
        CurrentStatus = GetMicrophoneStatus()
        if CurrentStatus == "True":
            MainExecution()
        
        else:
            AIStatus = GetAssistantStatus()
            if "Available..." in AIStatus:
                sleep(0.1)

            else:
                SetAssistantStatus("Available...")

def SecondThread():
    GraphicalUserInerface()

if __name__ == "__main__":
    thread2 = threading.Thread(target=FirstThread, daemon=True)
    thread2.start()
    SecondThread()

