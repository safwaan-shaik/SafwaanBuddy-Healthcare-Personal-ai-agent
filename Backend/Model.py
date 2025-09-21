import cohere # Brain of the Assistant
from rich import print #Nothing else than termina outputs
from dotenv import dotenv_values #to load environment variables from .env file
import ssl
import urllib3

# Disable SSL warnings and verification for corporate networks
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
ssl._create_default_https_context = ssl._create_unverified_context

env_vars = dotenv_values(".env") # Load environment variables from .env file

#Retrieve the API key from environment variables
CohereAPIKey = env_vars.get("CohereAPIKey")

# Initialize the Cohere client with the API key
try:
    co = cohere.Client(api_key=CohereAPIKey)
    print("✅ Cohere client initialized successfully")
except Exception as e:
    print(f"⚠️ Warning: Cohere client initialization failed: {e}")
    co = None

#define list of recognised keywords
funcs = [
    "exit", "general", "realtime", "open", "close", "play", "pause",
    "next", "previous", "volume up", "increase volume", "decrease volume","volume down",
    "generate image", "system", "content", "google search", "search", "youtube search", "reminder", "screenshot",
    "close window", "close tab", "minimize all", "shut down computer",
    # Healthcare commands
    "medication reminder", "take medication", "log symptom", "check lab results", 
    "emergency alert", "prenatal appointment", "upload prescription", "medication schedule", 
    "health summary", "health emergency", "prenatal care", "pregnancy care", "medication", 
    "health check", "doctor appointment", "call doctor", "contraction timer",
    # Enhanced features from AI-JARVIS integration
    "weather", "news", "headlines", "stock price", "share price", "tell joke", "joke",
    "wikipedia", "tell me about", "search wikipedia", "find location", "where is", 
    "location of", "cricket score", "cricket", "send email", "email", "current temperature"
]

#Empty list to store messages
messages = []

# Preamble for the Guiding the model
preamble = \
"""
You are a very accurate Decision-Making Model, who is made by HARSH KAAKADIYA, which decides what kind of a query is given to you.
You will decide whether a query is a 'general' query, a 'realtime' query, or is asking to perform any task or automation like 'open facebook, instagram', 'can you write a application and open it in notepad'
*** Do not answer any query, just decide what kind of query is given to you. ***
-> Respond with 'general ( query )' if a query can be answered by a llm model (conversational ai chatbot) and doesn't require any up to date information like if the query is 'who was akbar?' respond with 'general who was akbar?', if the query is 'how can i study more effectively?' respond with 'general how can i study more effectively?', if the query is 'can you help me with this math problem?' respond with 'general can you help me with this math problem?', if the query is 'Thanks, i really liked it.' respond with 'general thanks, i really liked it.' , if the query is 'what is python programming language?' respond with 'general what is python programming language?', etc. Respond with 'general (query)' if a query doesn't have a proper noun or is incomplete like if the query is 'who is he?' respond with 'general who is he?', if the query is 'what's his networth?' respond with 'general what's his networth?', if the query is 'tell me more about him.' respond with 'general tell me more about him.', and so on even if it require up-to-date information to answer. Respond with 'general (query)' if the query is asking about time, day, date, month, year, etc like if the query is 'what's the time?' respond with 'general what's the time?'.
-> Respond with 'realtime ( query )' if a query can not be answered by a llm model (because they don't have realtime data) and requires up to date information like if the query is 'who is indian prime minister' respond with 'realtime who is indian prime minister', if the query is 'tell me about facebook's recent update.' respond with 'realtime tell me about facebook's recent update.', if the query is 'tell me news about coronavirus.' respond with 'realtime tell me news about coronavirus.', etc and if the query is asking about any individual or thing like if the query is 'who is akshay kumar' respond with 'realtime who is akshay kumar', if the query is 'what is today's news?' respond with 'realtime what is today's news?', if the query is 'what is today's headline?' respond with 'realtime what is today's headline?', etc.
-> Respond with 'open (application name or website name)' if a query is asking to open any application like 'open facebook', 'open telegram', etc. but if the query is asking to open multiple applications, respond with 'open 1st application name, open 2nd application name' and so on.
-> Respond with 'close (application name)' if a query is asking to close any application like 'close notepad', 'close facebook', etc. but if the query is asking to close multiple applications or websites, respond with 'close 1st application name, close 2nd application name' and so on.
-> Respond with 'system play' or 'system pause' or 'system next' or 'system previous' if a query is asking to play, pause, next or previous any song like 'play afsanay by ys', 'pause the music', 'next song', etc. but if the query is asking to perform multiple tasks like 'play afsanay by ys and pause the music', respond with 'system play, system pause'.
-> Respond with 'play (song name)' if a query is asking to play any song like 'play afsanay by ys', 'play let her go', etc. but if the query is asking to play multiple songs, respond with 'play 1st song name, play 2nd song name' and so on.
-> Respond with 'generate image (image prompt)' if a query is requesting to generate a image with given prompt like 'generate image of a lion', 'generate image of a cat', etc. but if the query is asking to generate multiple images, respond with 'generate image 1st image prompt, generate image 2nd image prompt' and so on.
-> Respond with 'reminder (datetime with message)' if a query is requesting to set a reminder like 'set a reminder at 9:00pm on 25th june for my business meeting.' respond with 'reminder 9:00pm 25th june business meeting'.
-> Respond with 'system (task name)' if a query is asking to mute, unmute, volume up, volume down, full volume, max volume, increase the volume, increase volume a bit, decrease volume a bit. but if the query is asking to do multiple tasks, respond with 'system 1st task, system 2nd task', etc.
-> Respond with 'screenshot' if the user asks to take a screenshot, capture the screen, grab the screen, or take a snapshot (like 'take a screenshot', 'capture screen', 'screenshot now').
-> Respond with 'system minimize all' if a query is asking to minimize all windows like 'minimize all', 'minimize all windows', etc.
-> Respond with 'content (topic)' if a query is asking to write any type of content like application, codes, emails or anything else about a specific topic but if the query is asking to write multiple types of content, respond with 'content 1st topic, content 2nd topic' and so on.
-> Respond with 'google search (topic)' if a query is asking to search a specific topic on google but if the query is asking to search multiple topics on google, respond with 'google search 1st topic, google search 2nd topic' and so on.
-> Respond with 'youtube search (topic)' if a query is asking to search a specific topic on youtube or lecture topic or any topics of Study, but if the query is asking to search multiple topics on youtube, respond with 'youtube search 1st topic, youtube search 2nd topic' and so on.

*** HEALTHCARE COMMANDS ***
-> Respond with 'medication reminder (medication details)' for medication-related queries like 'remind me to take my prenatal vitamins', 'set medication reminder for iron tablets', etc.
-> Respond with 'take medication (medication name)' when user says they took medication like 'I took my vitamins', 'just took iron tablet', etc.
-> Respond with 'upload prescription' when user wants to upload or scan prescription like 'scan my prescription', 'upload prescription image', etc.
-> Respond with 'log symptom (symptom details)' when user reports symptoms like 'I'm feeling nauseous', 'having morning sickness', 'feeling contractions', etc.
-> Respond with 'check lab results' when user asks about lab results like 'show my blood test results', 'check my latest lab work', etc.
-> Respond with 'health emergency' for urgent medical concerns like 'I'm having severe contractions', 'emergency', 'call my doctor urgently', etc.
-> Respond with 'prenatal care (query)' for pregnancy-specific questions like 'when is my next appointment', 'pregnancy nutrition advice', etc.
-> Respond with 'contraction timer' when user mentions contractions timing like 'time my contractions', 'start contraction timer', etc.
-> Respond with 'call doctor' when user wants to contact healthcare provider like 'call my obstetrician', 'contact my doctor', etc.
-> Respond with 'pregnancy care (topic)' for general pregnancy care queries like 'pregnancy week information', 'baby development', etc.

*** ENHANCED FEATURES COMMANDS ***
-> Respond with 'weather (location)' for weather queries like 'what's the weather', 'weather in London', 'current temperature', etc.
-> Respond with 'news' or 'headlines' for news queries like 'latest news', 'top headlines', 'today's news', etc.
-> Respond with 'stock price (symbol)' for stock queries like 'Amazon stock price', 'AAPL share price', 'Tesla stock', etc.
-> Respond with 'tell joke' or 'joke' for joke requests like 'tell me a joke', 'make me laugh', 'something funny', etc.
-> Respond with 'wikipedia (topic)' or 'tell me about (topic)' for Wikipedia searches like 'tell me about Einstein', 'Wikipedia search for Python', etc.
-> Respond with 'find location (place)' or 'where is (place)' for location queries like 'where is Paris', 'location of Tokyo', etc.
-> Respond with 'cricket score' or 'cricket' for cricket information like 'cricket scores', 'current matches', etc.
-> Respond with 'send email (details)' for email requests like 'send an email', 'compose email', etc.

*** If the query is asking to perform multiple tasks like 'open facebook, telegram and close whatsapp' respond with 'open facebook, open telegram, close whatsapp' ***
*** If the query is asking to perform multiple tasks like 'open facebook, and continue music' respond with 'open facebook, pause'
*** If the query is asking to perform multiple tasks like 'open youtube, and play some music' respond with 'open youtube, play Allah ke Bande"
*** If the user is saying goodbye or wants to end the conversation like 'aisha terminate' respond with 'exit'.***
*** Respond with 'general (query)' if you can't decide the kind of query or if a query is asking to perform a task which is not mentioned above. ***
"""

ChatHistory = [
    {"role": "User", "message": "how are you?"},
    {"role": "Chatbot", "message": "general how are you"},
    {"role": "User", "message": "do you like pizza?"},
    {"role": "Chatbot", "message": "general do you like pizza?"},
    {"role": "User", "message": "open Edge and Onenote"},
    {"role":"Chatbot", "message": "open Edge, open onenote"},
    {"role": "User", "message": "open Edge and tell me about Bhagat Singh Ji."},
    {"role":"Chatbot", "message": "open Edge, general tell me about Bhagat Singh Ji."},
    {"role": "User", "message": "what is today's date and by the way remind me that i have to take my tablet on 19th June at 8pm."},
    {"role":"Chatbot", "message": "general what is today's date, reminder 8:00PM 19th June take tablet."},
    {"role":"User", "message": "chat with me."},
    {"role":"Chatbot", "message":"general chat with me."}
]

def FirstLayerDMM(prompt: str = "test"):  # sourcery skip: use-join
    #Add The user's query to the messages list.
    messages.append({"role":"user", "content": f"{prompt}"})
    
    if co is None:
        print("Warning: Cohere client not available, using fallback decision making")
        # Simple fallback decision making
        prompt_lower = prompt.lower()
        if any(word in prompt_lower for word in ["open", "launch", "start"]):
            return ["open " + prompt.replace("open", "").replace("launch", "").replace("start", "").strip()]
        elif any(word in prompt_lower for word in ["close", "exit", "quit"]):
            return ["close " + prompt.replace("close", "").replace("exit", "").replace("quit", "").strip()]
        elif any(word in prompt_lower for word in ["search", "google", "find"]):
            return ["google search " + prompt]
        elif any(word in prompt_lower for word in ["play", "music", "song"]):
            return ["play " + prompt.replace("play", "").strip()]
        else:
            return ["general " + prompt]

    try:
        #create a streaming chat session.
        stream = co.chat_stream(
            model="command-r-plus",
            message=prompt,
            temperature=0.7,
            chat_history=ChatHistory,
            prompt_truncation="OFF",
            connectors=[],
            preamble=preamble)

        # init an empty string to store the response
        response = ""
        # Iterate through the stream to get the response

        for event in stream:
            if event.event_type == "text-generation":
                response += event.text #Appends generated text to the response string

        # REmove newline characters and split into individual commands
        response = response.replace("\n", "")
        response= response.split(",")

        response = [i.strip() for i in response]

        #list filter valid tasks
        temp = []

        for task in response:
            for func in funcs:
                if task.startswith(func):
                    temp.append(task)
        response = temp
        
        # If the response contains "(query)", it indicates a query for the model
        if "(query)" in response:
            newresponse = FirstLayerDMM(prompt=prompt)
            return newresponse
        else:
            return response
    
    except Exception as e:
        print(f"Error in FirstLayerDMM: {e}")
        # Fallback to simple decision making
        prompt_lower = prompt.lower()
        if any(word in prompt_lower for word in ["open", "launch", "start"]):
            return ["open " + prompt.replace("open", "").replace("launch", "").replace("start", "").strip()]
        elif any(word in prompt_lower for word in ["hello", "hi", "hey"]):
            return ["general " + prompt]
        else:
            return ["general " + prompt]

if __name__ == "__main__":
    while True:
        print(FirstLayerDMM(input(">>> ")))
