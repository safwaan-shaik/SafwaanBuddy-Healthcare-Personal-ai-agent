"""
GOogle search the Answer, and  then Compress it using the Groq AI.
the ChatBot just gives a General Answer of query
Here we are searching the realtime data, and giving it to chatbot to summarise
"""
from googlesearch import search
from groq import Groq
from json import dump, load
import datetime
from dotenv import dotenv_values
import requests
from bs4 import BeautifulSoup
import re

env_vars = dotenv_values(".env")  # Load environment variables from .env file
Username = env_vars.get("Username")
Assistantname = env_vars.get("Assistantname")
GroqAPIKey = env_vars.get("GroqAPIKey")
GOOGLE_API_KEY = env_vars.get("GOOGLE_API_KEY")
SEARCH_ENGINE_ID = env_vars.get("SEARCH_ENGINE_ID")

# Init the Groq
client = Groq(api_key=GroqAPIKey)

System = f"""Hello, I am {Username}, You are a very accurate and advanced AI chatbot named {Assistantname} which has real-time up-to-date information from the internet.
*** Provide Answers In a Professional Way, make sure to add full stops, commas, question marks, and use proper grammar.***
*** Just answer the question from the provided data in a professional way. ***"""

try:
    with open(r'Data\ChatLog.json', 'r') as file:
        messages = load(file)
except FileNotFoundError:
    with open(r'Data\ChatLog.json', 'w') as file:
        dump([], file)

def _clean_text(text: str) -> str:
    if not text:
        return ""
    text = re.sub(r"\s+", " ", text)
    return text.strip()

def _fallback_scrape(query: str, max_results: int = 5):
    results = []
    try:
        for url in search(query, num_results=max_results):  # googlesearch-python
            try:
                r = requests.get(url, timeout=6, headers={
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                })
                if r.status_code != 200:
                    continue
                soup = BeautifulSoup(r.text, 'html.parser')
                title = soup.title.string if soup.title else url
                # Grab first meaningful paragraph/text chunk
                para = ''
                for p in soup.find_all(['p']):
                    txt = _clean_text(p.get_text())
                    if len(txt.split()) > 5:
                        para = txt
                        break
                if not para:
                    para = "No snippet available."
                results.append({"title": _clean_text(title), "snippet": para})
            except Exception:
                continue
            if len(results) >= max_results:
                break
    except Exception:
        pass
    return results

def GoogleSearch(query):
    """Primary: Google Custom Search API. Fallback: lightweight scraping via googlesearch-python."""
    Answer = f"'{query}' are:\n[start]\n"

    # If key or engine id missing, skip directly to fallback.
    if not GOOGLE_API_KEY or not SEARCH_ENGINE_ID:
        print("[RealtimeSearch] Missing GOOGLE_API_KEY or SEARCH_ENGINE_ID – using fallback search.")
        fallback = _fallback_scrape(query)
        if fallback:
            for item in fallback:
                Answer += f"Title: {item['title']}\nDescription: {item['snippet']}\n\n"
        else:
            Answer += "No results found.\n"
        Answer += "[end]"
        return Answer

    try:
        url = "https://www.googleapis.com/customsearch/v1"
        params = {"key": GOOGLE_API_KEY, "cx": SEARCH_ENGINE_ID, "q": query, "num": 5}
        response = requests.get(url, params=params, timeout=8)
        status = response.status_code
        data = {}
        try:
            data = response.json()
        except Exception:
            print("[RealtimeSearch] Non-JSON response from Custom Search API.")

        if status != 200:
            print(f"[RealtimeSearch] Custom Search HTTP {status}: {data}")
        if 'error' in data:
            print(f"[RealtimeSearch] API error: {data['error']}")

        if "items" in data and data["items"]:
            for item in data["items"]:
                title = item.get("title", "No Title")
                snippet = item.get("snippet", "No Description")
                Answer += f"Title: {title}\nDescription: {snippet}\n\n"
        else:
            print("[RealtimeSearch] No items returned – invoking fallback scrape.")
            fallback = _fallback_scrape(query)
            if fallback:
                for item in fallback:
                    Answer += f"Title: {item['title']}\nDescription: {item['snippet']}\n\n"
            else:
                Answer += "No results found.\n"
    except Exception as e:
        print(f"[RealtimeSearch] Exception during search: {e}")
        fallback = _fallback_scrape(query)
        if fallback:
            for item in fallback:
                Answer += f"Title: {item['title']}\nDescription: {item['snippet']}\n\n"
        else:
            Answer += "No results found.\n"

    Answer += "[end]"
    return Answer

def AnswerModifier(answer):
    lines = answer.split('\n')
    non_empty_lines = [line for line in lines if line.strip()]
    return '\n'.join(non_empty_lines)  # Modified Answer.

SystemChatBot = [
    {"role": "system", "content": System},
    {'role': 'user', 'content': 'Hi'},
    {"role": "assistant", "content": "Hello, how can I help you?"}
]

def Info():
    data = ""
    current_datetime = datetime.datetime.now()
    day = current_datetime.strftime("%A")
    date = current_datetime.strftime("%d")
    month = current_datetime.strftime("%B")
    year = current_datetime.strftime("%Y")
    hour = current_datetime.strftime("%I")
    minute = current_datetime.strftime("%M")

    data += f"Use this real-time information if needed:\n"
    data += f"Day: {day}\nDate: {date}\nMonth: {month}\nYear: {year}\n"
    data += f"Time: {hour} hours, {minute} minutes\n"
    return data

def RealtimeSearchEngine(prompt):  # sourcery skip: use-join
    global SystemChatBot, messages
    with open(r'Data/ChatLog.json', 'r') as file:
        messages = load(file)
    messages.append({"role": "user", "content": f"{prompt}"})

    search_result = GoogleSearch(prompt)
    if "No results found" in search_result:
        return "I couldn't find reliable web results for that query right now. Try rephrasing or a more specific topic."

    SystemChatBot.append({"role": "system", "content": search_result})

    completion = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=SystemChatBot + [{"role" : "system", "content" : Info()}] + messages,
        temperature=0.7,
        max_tokens= 2048,
        top_p=1,
        stream=True,
        stop=None
    )

    Answer = ""

    for chunk in completion:
        if chunk.choices[0].delta.content:
            Answer += chunk.choices[0].delta.content
        
    Answer = Answer.strip().replace("</s>", "")
    messages.append({"role": "assistant" , "content":Answer})

    with open(r"Data/ChatLog.json", 'w') as file:
        dump(messages, file, indent=4)

    SystemChatBot.pop()
    return AnswerModifier(answer=Answer)


if __name__ == "__main__":
    while True:
        prompt = input("\nEnter your Query: ")
        print(RealtimeSearchEngine(prompt))