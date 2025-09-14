from AppOpener import close,open as appopen
from webbrowser import open as webopen
from pywhatkit import search, playonyt
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
from ctypes import cast, POINTER
from comtypes import CLSCTX_ALL
from pycaw.pycaw import AudioUtilities, IAudioEndpointVolume

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
    if command == 'shut down computer':
        os.system("shutdown /s /t 1")
    elif command == 'minimize all':
        keyboard.send('win + d')

    elif command == 'pause' or command == 'play' or command.__contains__("continue"):
        pyautogui.press('playpause')
        
def GenerateImage(Topic):
    Topic = Topic.replace("generate image ", "").replace("of this", "")
    with open(r"D:\J.A.R.V.i.S - Copy\J.A.R.V.I.S-A.I\Frontend\Files\ImageGeneration.data", "w") as f:
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

