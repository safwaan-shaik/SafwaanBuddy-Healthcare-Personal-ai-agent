"""
J.A.R.V.I.S Enhanced Features Module
Additional functionalities inspired by AI-JARVIS repository integration
Weather, News, Stocks, Email, Jokes, Wikipedia, Cricket, and Location services
"""

import requests
import json
import smtplib
import random
import wikipedia
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime
from typing import Dict, Any, Optional, List
import geocoder
import re
from bs4 import BeautifulSoup
from dotenv import dotenv_values

# Load environment variables
env_vars = dotenv_values('.env')

class WeatherService:
    """Weather information service using OpenWeatherMap API"""
    
    def __init__(self):
        self.api_key = env_vars.get('OPENWEATHER_API_KEY', 'demo_key')
        self.base_url = "http://api.openweathermap.org/data/2.5/weather"
        
    def get_weather(self, city: str = None) -> str:
        """Get current weather for a city"""
        try:
            if not city:
                # Try to get user's current location
                g = geocoder.ip('me')
                if g.city:
                    city = g.city
                else:
                    city = "London"  # Default city
            
            if self.api_key == 'demo_key':
                # Demo response when no API key is available
                return f"Weather in {city}: It's a beautiful day with moderate temperature. Consider getting an OpenWeatherMap API key for real weather data."
            
            params = {
                'q': city,
                'appid': self.api_key,
                'units': 'metric'
            }
            
            response = requests.get(self.base_url, params=params, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                temp = data['main']['temp']
                feels_like = data['main']['feels_like']
                humidity = data['main']['humidity']
                description = data['weather'][0]['description']
                city_name = data['name']
                country = data['sys']['country']
                
                weather_report = f"""Weather in {city_name}, {country}:
                Temperature: {temp}°C (feels like {feels_like}°C)
                Condition: {description.title()}
                Humidity: {humidity}%"""
                
                return weather_report
            else:
                return f"Sorry, I couldn't fetch weather information for {city}. Please check the city name."
                
        except Exception as e:
            return f"Error getting weather information: {str(e)}"

class NewsService:
    """News headlines service using NewsAPI"""
    
    def __init__(self):
        self.api_key = env_vars.get('NEWS_API_KEY', 'demo_key')
        self.base_url = "https://newsapi.org/v2/top-headlines"
    
    def get_top_news(self, country: str = 'us', category: str = None) -> str:
        """Get top news headlines"""
        try:
            if self.api_key == 'demo_key':
                # Demo news when no API key is available
                demo_news = [
                    "Tech companies report strong quarterly earnings",
                    "Climate change summit reaches new agreements",
                    "Space exploration mission achieves new milestones",
                    "Healthcare innovations show promising results",
                    "Economic markets show steady growth"
                ]
                return "Here are today's top headlines:\n" + "\n".join([f"{i+1}. {news}" for i, news in enumerate(demo_news)])
            
            params = {
                'apiKey': self.api_key,
                'country': country,
                'pageSize': 5
            }
            
            if category:
                params['category'] = category
                
            response = requests.get(self.base_url, params=params, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                articles = data['articles']
                
                if articles:
                    news_list = []
                    for i, article in enumerate(articles[:5], 1):
                        title = article['title']
                        news_list.append(f"{i}. {title}")
                    
                    return "Here are today's top headlines:\n" + "\n".join(news_list)
                else:
                    return "No news articles found."
            else:
                return "Sorry, I couldn't fetch the latest news at the moment."
                
        except Exception as e:
            return f"Error getting news: {str(e)}"

class StockService:
    """Stock price checking service"""
    
    def __init__(self):
        self.alpha_vantage_key = env_vars.get('ALPHA_VANTAGE_API_KEY', 'demo_key')
    
    def get_stock_price(self, symbol: str) -> str:
        """Get current stock price for a symbol"""
        try:
            symbol = symbol.upper()
            
            if self.alpha_vantage_key == 'demo_key':
                # Demo stock prices
                demo_prices = {
                    'AAPL': '$150.25 (+2.15%)',
                    'GOOGL': '$2,845.67 (+1.23%)',
                    'AMZN': '$3,234.89 (-0.45%)',
                    'MSFT': '$342.56 (+0.87%)',
                    'TSLA': '$756.34 (+3.21%)'
                }
                price = demo_prices.get(symbol, '$XXX.XX (±X.XX%)')
                return f"{symbol} stock price: {price} (Demo data - Get Alpha Vantage API key for real data)"
            
            url = f"https://www.alphavantage.co/query"
            params = {
                'function': 'GLOBAL_QUOTE',
                'symbol': symbol,
                'apikey': self.alpha_vantage_key
            }
            
            response = requests.get(url, params=params, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if 'Global Quote' in data:
                    quote = data['Global Quote']
                    price = quote['05. price']
                    change = quote['09. change']
                    change_percent = quote['10. change percent']
                    
                    return f"{symbol} stock price: ${float(price):.2f} ({change_percent})"
                else:
                    return f"Sorry, I couldn't find stock information for {symbol}."
            else:
                return "Sorry, I couldn't fetch stock information at the moment."
                
        except Exception as e:
            return f"Error getting stock price: {str(e)}"

class EmailService:
    """Email sending service"""
    
    def __init__(self):
        self.smtp_server = env_vars.get('SMTP_SERVER', 'smtp.gmail.com')
        self.smtp_port = int(env_vars.get('SMTP_PORT', '587'))
        self.email = env_vars.get('USER_EMAIL')
        self.password = env_vars.get('USER_EMAIL_PASSWORD')
    
    def send_email(self, to_email: str, subject: str, message: str) -> str:
        """Send an email"""
        try:
            if not self.email or not self.password:
                return "Email credentials not configured. Please set USER_EMAIL and USER_EMAIL_PASSWORD in .env file."
            
            # Create message
            msg = MIMEMultipart()
            msg['From'] = self.email
            msg['To'] = to_email
            msg['Subject'] = subject
            
            # Add body to email
            msg.attach(MIMEText(message, 'plain'))
            
            # Create SMTP session
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls()  # Enable TLS
            server.login(self.email, self.password)
            
            # Send email
            text = msg.as_string()
            server.sendmail(self.email, to_email, text)
            server.quit()
            
            return f"Email sent successfully to {to_email}"
            
        except Exception as e:
            return f"Error sending email: {str(e)}"

class JokeService:
    """Joke telling service"""
    
    def __init__(self):
        self.jokes_api = "https://official-joke-api.appspot.com/random_joke"
        
        # Backup jokes in case API is not available
        self.backup_jokes = [
            {"setup": "Why don't scientists trust atoms?", "punchline": "Because they make up everything!"},
            {"setup": "What do you call a fake noodle?", "punchline": "An impasta!"},
            {"setup": "Why did the scarecrow win an award?", "punchline": "He was outstanding in his field!"},
            {"setup": "What do you call a bear with no teeth?", "punchline": "A gummy bear!"},
            {"setup": "Why don't eggs tell jokes?", "punchline": "They'd crack each other up!"},
            {"setup": "What's the best thing about Switzerland?", "punchline": "I don't know, but the flag is a big plus!"},
            {"setup": "Why did the math book look so sad?", "punchline": "Because it was full of problems!"}
        ]
    
    def get_joke(self) -> str:
        """Get a random joke"""
        try:
            response = requests.get(self.jokes_api, timeout=5)
            
            if response.status_code == 200:
                joke_data = response.json()
                setup = joke_data.get('setup', '')
                punchline = joke_data.get('punchline', '')
                return f"{setup}\n{punchline}"
            else:
                # Use backup joke
                joke = random.choice(self.backup_jokes)
                return f"{joke['setup']}\n{joke['punchline']}"
                
        except Exception:
            # Use backup joke
            joke = random.choice(self.backup_jokes)
            return f"{joke['setup']}\n{joke['punchline']}"

class WikipediaService:
    """Wikipedia search service"""
    
    def search_wikipedia(self, query: str) -> str:
        """Search Wikipedia for information"""
        try:
            # Set language to English
            wikipedia.set_lang("en")
            
            # Search for the query
            search_results = wikipedia.search(query, results=3)
            
            if not search_results:
                return f"Sorry, I couldn't find any Wikipedia articles about '{query}'."
            
            # Get the first result
            try:
                page = wikipedia.page(search_results[0])
                
                # Get a summary (first 3 sentences)
                summary = wikipedia.summary(search_results[0], sentences=3)
                
                return f"According to Wikipedia:\n\n{summary}\n\nWould you like to know more about this topic?"
                
            except wikipedia.exceptions.DisambiguationError as e:
                # Handle disambiguation
                return f"There are multiple articles about '{query}'. Did you mean: {', '.join(e.options[:5])}?"
                
            except wikipedia.exceptions.PageError:
                return f"Sorry, I couldn't find a Wikipedia page for '{query}'."
                
        except Exception as e:
            return f"Error searching Wikipedia: {str(e)}"

class LocationService:
    """Location and places service"""
    
    def __init__(self):
        self.geocoding_api = "https://api.opencagedata.com/geocode/v1/json"
        self.api_key = env_vars.get('OPENCAGE_API_KEY', 'demo_key')
    
    def find_location(self, place: str) -> str:
        """Find location information for a place"""
        try:
            if self.api_key == 'demo_key':
                # Demo response
                return f"Location: {place}\nI would need an OpenCage API key to provide detailed location information including coordinates, country, and nearby places."
            
            params = {
                'q': place,
                'key': self.api_key,
                'limit': 1,
                'no_annotations': 1
            }
            
            response = requests.get(self.geocoding_api, params=params, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if data['results']:
                    result = data['results'][0]
                    
                    formatted_address = result['formatted']
                    geometry = result['geometry']
                    lat = geometry['lat']
                    lng = geometry['lng']
                    
                    location_info = f"""Location: {place}
                    Address: {formatted_address}
                    Coordinates: {lat}, {lng}"""
                    
                    return location_info
                else:
                    return f"Sorry, I couldn't find location information for '{place}'."
            else:
                return "Sorry, I couldn't access location services at the moment."
                
        except Exception as e:
            return f"Error finding location: {str(e)}"

class CricketService:
    """Cricket scores and information service"""
    
    def __init__(self):
        self.cricket_api = "https://api.cricapi.com/v1/currentMatches"
        self.api_key = env_vars.get('CRICKET_API_KEY', 'demo_key')
    
    def get_cricket_scores(self) -> str:
        """Get current cricket match scores"""
        try:
            if self.api_key == 'demo_key':
                # Demo cricket scores
                demo_matches = [
                    "IND vs AUS: India 287/6 (45.2 overs) - Live",
                    "ENG vs PAK: England won by 5 wickets", 
                    "SA vs NZ: South Africa 156/4 (28 overs) - Live"
                ]
                return "Current Cricket Matches:\n" + "\n".join([f"{i+1}. {match}" for i, match in enumerate(demo_matches)])
            
            params = {
                'apikey': self.api_key,
                'offset': 0
            }
            
            response = requests.get(self.cricket_api, params=params, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                
                if data.get('data'):
                    matches = data['data'][:5]  # Get first 5 matches
                    
                    match_info = []
                    for i, match in enumerate(matches, 1):
                        teams = f"{match.get('teamInfo', [{}])[0].get('shortname', 'Team1')} vs {match.get('teamInfo', [{}])[1].get('shortname', 'Team2') if len(match.get('teamInfo', [])) > 1 else 'Team2'}"
                        status = match.get('status', 'Status unknown')
                        match_info.append(f"{i}. {teams}: {status}")
                    
                    return "Current Cricket Matches:\n" + "\n".join(match_info)
                else:
                    return "No cricket matches are currently being played."
            else:
                return "Sorry, I couldn't fetch cricket scores at the moment."
                
        except Exception as e:
            return f"Error getting cricket scores: {str(e)}"

# Global service instances
weather_service = WeatherService()
news_service = NewsService()
stock_service = StockService()
email_service = EmailService()
joke_service = JokeService()
wikipedia_service = WikipediaService()
location_service = LocationService()
cricket_service = CricketService()

# Main function handlers for integration with existing automation
def get_weather_info(location: str = None) -> str:
    """Get weather information"""
    return weather_service.get_weather(location)

def get_news_headlines(category: str = None) -> str:
    """Get top news headlines"""
    return news_service.get_top_news(category=category)

def get_stock_price(symbol: str) -> str:
    """Get stock price"""
    return stock_service.get_stock_price(symbol)

def send_email_message(to_email: str, subject: str, message: str) -> str:
    """Send email"""
    return email_service.send_email(to_email, subject, message)

def tell_joke() -> str:
    """Tell a joke"""
    return joke_service.get_joke()

def search_wikipedia_info(query: str) -> str:
    """Search Wikipedia"""
    return wikipedia_service.search_wikipedia(query)

def find_place_location(place: str) -> str:
    """Find location"""
    return location_service.find_location(place)

def get_cricket_info() -> str:
    """Get cricket scores"""
    return cricket_service.get_cricket_scores()

# Utility function to parse commands and extract relevant information
def parse_enhanced_command(command: str) -> tuple:
    """Parse enhanced commands to extract action and parameters"""
    command = command.lower().strip()
    
    if "weather" in command:
        # Extract location if mentioned
        location_match = re.search(r'weather (?:in |of |for )?(.+)', command)
        location = location_match.group(1) if location_match else None
        return "weather", location
    
    elif "news" in command:
        # Extract category if mentioned
        category_match = re.search(r'(technology|sports|business|health|science|entertainment) news', command)
        category = category_match.group(1) if category_match else None
        return "news", category
    
    elif "stock" in command or "share price" in command:
        # Extract stock symbol
        stock_match = re.search(r'(?:stock|share price|price of) (?:of )?([a-zA-Z]+)', command)
        symbol = stock_match.group(1) if stock_match else "AAPL"
        return "stock", symbol
    
    elif "email" in command:
        # This would need more complex parsing for email details
        return "email", command
    
    elif "joke" in command:
        return "joke", None
    
    elif "wikipedia" in command or "tell me about" in command:
        # Extract search query
        wiki_match = re.search(r'(?:wikipedia|tell me about|search for) (.+)', command)
        query = wiki_match.group(1) if wiki_match else command
        return "wikipedia", query
    
    elif "location" in command or "where is" in command:
        # Extract place name
        location_match = re.search(r'(?:location of|where is|find) (.+)', command)
        place = location_match.group(1) if location_match else command
        return "location", place
    
    elif "cricket" in command or "score" in command:
        return "cricket", None
    
    return "unknown", command