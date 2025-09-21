from AppOpener import close,open as appopen
from webbrowser import open as webopen
# Handle SSL issues with pywhatkit import
try:
    from pywhatkit import search, playonyt
except Exception as e:
    print(f"Warning: pywhatkit import failed due to SSL issue: {e}")
    # Define fallback functions
    def search(query):
        import webbrowser
        webbrowser.open(f"https://www.google.com/search?q={query}")
    def playonyt(query):
        import webbrowser
        webbrowser.open(f"https://www.youtube.com/results?search_query={query}")
from dotenv import dotenv_values
from bs4 import BeautifulSoup
from rich import print
from groq import Groq
import webbrowser
import requests
import subprocess
import keyboard
import pyautogui
import asyncio
import os
import datetime
# Audio control imports - fallback to keyboard shortcuts if admin access not available
try:
    from ctypes import cast, POINTER
    from comtypes import CLSCTX_ALL
    from pycaw.pycaw import AudioUtilities, IAudioEndpointVolume
    AUDIO_CONTROL_AVAILABLE = True
except Exception as e:
    print(f"Note: Advanced audio control not available without admin access: {e}")
    print("Using keyboard shortcuts for audio control instead.")
    AUDIO_CONTROL_AVAILABLE = False

# Import enhanced features
try:
    from Backend.EnhancedFeatures import (
        get_weather_info, get_news_headlines, get_stock_price, 
        send_email_message, tell_joke, search_wikipedia_info,
        find_place_location, get_cricket_info, parse_enhanced_command
    )
    ENHANCED_FEATURES_AVAILABLE = True
    print("✅ Enhanced features loaded successfully")
except ImportError as e:
    print(f"⚠️ Enhanced features not available: {e}")
    ENHANCED_FEATURES_AVAILABLE = False

"------------------------------------------------------------------------------------------------------------------------------------"
env_vars = dotenv_values(".env")
GroqAPIKey = env_vars.get("GroqAPIKey")

#Define CSS Classes for parsing specific elements in HTML contnent
classes = [ "zCubwf", "hgKElc", "LTKOO sY7ric", "Z0LcW", "gsrt vk_bk FzvWSb YwPhnf", "pclqee", "tw-Data-text tw-text-small tw-ta", "IZ6rdc", "O5uR6d LTKOO",
           "vlzY6d", "webanswer-webanswers_table__webanswer-table", "dDoNo ikb4Bb gsrt", "sXLaOe",
           "LWkfKe", "VQF4g", "qv3Wpe", "kno-rdesc", "SPZz6b"]

useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36"

client = Groq(api_key=GroqAPIKey)

professional_responses = [
    "Your satisfaction is our priority. If you have any further questions or need additional assistance, just ask",
    "I am in your hands, please let me know how I can assist you further",
]

messages = []

SystemChatBot = [{"role": "system", "content": f"Hello, I am {os.environ['Username']}, You are a content writer. You have to write content like letters, paragraphs, reports, essays, etc."}]

def GoogleSearch(Topic):
    search(Topic)
    return True

def Content(Topic):

    def OpenNotepad(File):
        default_text_editor = 'notepad.exe'
        subprocess.Popen([default_text_editor, File])

    def ContentWriterAI(prompt):
        messages.append({"role": "user", "content": f"{prompt}"})

        completion = client.chat.completions.create(
            model='qwen/qwen3-32b',
            messages=messages,
            max_tokens=2048,
            temperature=0.7,
            top_p=1,
            stream=True,
            stop=None
        )
        Answer = ""

        for chunck in completion:
            if chunck.choices[0].delta.content:
                Answer += chunck.choices[0].delta.content
        
        Answer = Answer.replace("</s>", "")
        messages.append({"role": "assistant", "content": Answer})
        return Answer

    Topic: str = Topic.replace("Content ", "")
    ContentByAI = ContentWriterAI(Topic)

    filename = rf"Data\{Topic.lower().replace(' ', '_')}.txt"
    with open(filename, "w", encoding="utf-8") as file:
        file.write(ContentByAI)
        file.close()
    
    OpenNotepad(filename)
    return True

def YoutubeSearch(Topic):
    Url4srch = f"https://www.youtube.com/results?search_query={Topic}"
    webbrowser.open(Url4srch)
    return True

def PlayOnYoutube(query):
    playonyt(query)
    return True

def OpenApp(app, sess=requests.Session()):
    try:
        appopen(app, match_closest=True, output=True, throw_error=True)
        return True
    except:
        def extract_links(html):
            if html is None:
                return []
            soup = BeautifulSoup(html, 'html.parser')
            links = soup.find_all('a', {'jsname': 'UWckNb'})
            return [link.get('href') for link in links]
        def search_google(query):
            url = f"https://www.google.com/search?q={query}"
            headers = {"user-Agent": useragent}
            response = sess.get(url, headers=headers)
            if response.status_code == 200:
                return response.text
            else:
                print("Failed to fetch search results")
                return None
        html = search_google(app)

        if html:
            links = extract_links(html)
            if links:
                link = links[0]
                webopen(link)
                return True
            else:
                # No links found, open Google search page
                search_url = f"https://www.google.com/search?q={app}"
                webbrowser.open(search_url)
                return True
        
def CloseApp(app):

    if 'chrome' in app:
        pass # Don't close the Browser.
    else:
        try:
            close(app, match_closest=True, output=True, throw_error=True)
            return True
        except:
            return False

def System(command):

    def mute():
        keyboard.press_and_release('volume mute')
        
    def unmute():
        keyboard.press_and_release('volume mute')
    def volume_up():
        keyboard.press_and_release('volume up')
    def volume_down():
        keyboard.press_and_release('volume down')

    if command == 'mute':
        mute()
    elif command == 'unmute':
        unmute()
    elif command in ('volume up', 'increase volume'):
        volume_up()
    elif command in ('volume down', 'decrease volume'):
        volume_down()
    elif command == 'shut down computer':
        # Use user-level shutdown command that doesn't require admin
        try:
            # First try user-level shutdown
            os.system("shutdown /s /t 60 /c \"J.A.R.V.I.S. shutdown requested\"")
            return "Shutdown initiated. The computer will shutdown in 60 seconds. You can cancel with 'shutdown /a' if needed."
        except Exception as e:
            return "I don't have permission to shutdown the computer. Please do it manually or run with administrator privileges."
    elif command == 'minimize all':
        keyboard.send('win + d')

    elif command == 'pause' or command == 'play' or command.__contains__("continue"):
        pyautogui.press('playpause')
        
def GenerateImage(Topic):
    Topic = Topic.replace("generate image ", "").replace("of this", "")
    # Use relative path instead of hardcoded path
    image_data_file = os.path.join("Data", "ImageGeneration.data")
    os.makedirs("Data", exist_ok=True)  # Ensure Data directory exists
    with open(image_data_file, "w") as f:
        f.write(f"{Topic},true")
    return True

def Screenshot(_):
    """Capture a full-screen screenshot and save it under Data/ with timestamp."""
    ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = rf"Data\screenshot_{ts}.png"
    try:
        img = pyautogui.screenshot()
        img.save(filename)
        print(f"[Automation] Screenshot saved: {filename}")
        return True
    except Exception as e:
        print(f"[Automation] Screenshot failed: {e}")
        return False

def HandleEnhancedFeatures(command):
    """Handle enhanced features from AI-JARVIS integration"""
    if not ENHANCED_FEATURES_AVAILABLE:
        return "Enhanced features not available."
    
    try:
        action, parameter = parse_enhanced_command(command)
        
        if action == "weather":
            return get_weather_info(parameter)
        elif action == "news":
            return get_news_headlines(parameter)
        elif action == "stock":
            return get_stock_price(parameter)
        elif action == "joke":
            return tell_joke()
        elif action == "wikipedia":
            return search_wikipedia_info(parameter)
        elif action == "location":
            return find_place_location(parameter)
        elif action == "cricket":
            return get_cricket_info()
        elif action == "email":
            # For email, we'd need to parse more details
            return "Email functionality available. Please specify recipient, subject, and message."
        else:
            return f"Enhanced feature '{action}' not implemented yet."
            
    except Exception as e:
        print(f"Error in enhanced features: {e}")
        return f"Error processing enhanced feature: {str(e)}"

async def TranslateAndExecute(commands: list[str]):
    """
    "exit", "general", "realtime", "open", "close", "play", "pause",
    "next", "previous", "volume up", "increase volume", "decrease volume","volume down",
    "generate image", "system", "content", "google search", "search", "youtube search",
    "reminder",

    "close window", "close tab", "minimize all", "shut down computer"
    """
    funcs = []
    for command in commands:

        if command.startswith("open"):
            if "open it" in command:
                pass
            if "open file" == command:
                pass
            else:
                fun = asyncio.to_thread(OpenApp, command.removeprefix("open "))
                funcs.append(fun)

        elif command.startswith("general"):
            pass

        elif command.startswith("realtime"):
            pass

        elif command.startswith("close "):
            fun = asyncio.to_thread(CloseApp, command.removeprefix("close "))
            funcs.append(fun)

        elif command.startswith("play "):
            fun = asyncio.to_thread(PlayOnYoutube, command.removeprefix("play "))
            funcs.append(fun)

        elif command.startswith("content "):
            fun = asyncio.to_thread(Content, command.removeprefix("content "))
            funcs.append(fun)

        elif command.startswith("google ") or command.startswith("google search "):
            fun = asyncio.to_thread(GoogleSearch, command.removeprefix("google ").removeprefix("search "))
            funcs.append(fun)

        elif command.startswith("youtube search "):
            fun = asyncio.to_thread(YoutubeSearch, command.removeprefix("youtube search "))
            funcs.append(fun)
        
        elif command.startswith("system "):
            fun = asyncio.to_thread(System, command.removeprefix("system "))
            funcs.append(fun)

        elif command.startswith("generate image"):
            fun = asyncio.to_thread(GenerateImage, command.removeprefix("generate image "))
            funcs.append(fun)

        elif command.startswith("screenshot"):
            fun = asyncio.to_thread(Screenshot, command)
            funcs.append(fun)

        # Enhanced features integration
        elif ENHANCED_FEATURES_AVAILABLE and any(command.startswith(cmd) for cmd in ['weather', 'news', 'headlines', 'stock price', 'share price', 'tell joke', 'joke', 'wikipedia', 'tell me about', 'find location', 'where is', 'location of', 'cricket score', 'cricket', 'send email', 'email']):
            fun = asyncio.to_thread(HandleEnhancedFeatures, command)
            funcs.append(fun)

        else:
            print(f"NO function found for command: {command}")
    
    results = await asyncio.gather(*funcs)

    for result in results:
        if isinstance(result, str):
            yield result
        else:
            yield str(result)
    
async def Automation(commands: list[str]):
    async for result in TranslateAndExecute(commands):
        pass
    return True

