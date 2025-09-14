# J.A.R.V.I.S - Your Personal AI Assistant

![J.A.R.V.I.S. GUI](https://i.imgur.com/example.png) <!-- Replace with a real screenshot link if you have one -->

J.A.R.V.I.S. is a sophisticated, voice-activated personal AI assistant built with Python. It integrates multiple cutting-edge AI services to provide a seamless, conversational experience for managing tasks, answering questions, and controlling your system.

## ✨ Key Features

- **Voice-Activated Control**: Hands-free operation for all commands.
- **Conversational AI Chat**: Powered by state-of-the-art language models via the Groq API for fast, intelligent responses.
- **Real-Time Information**: Fetches and summarizes real-time information from the web using Google's Custom Search API.
- **AI-Powered Image Generation**: Creates images from text prompts using Google's Gemini API.
- **System Automation**:
    - **Application Management**: Open and close applications installed on your system.
    - **File & Folder Access**: Open any file or folder on your computer with a simple voice command.
    - **System Controls**: Adjust volume, play/pause media, and take screenshots.
- **Intelligent Task Routing**: A smart decision-making layer using Cohere's API to understand user intent and trigger the correct function.
- **Interactive GUI**: A clean and modern graphical user interface built with PyQt5, providing visual feedback and controls.

## 🛠️ Technologies Used

- **Backend**: Python
- **GUI**: PyQt5
- **AI & Language Models**:
    - **Chat**: Groq (Llama 3.1)
    - **Decision Making**: Cohere
    - **Image Generation**: Google Gemini
    - **Real-Time Search**: Google Custom Search API
- **Speech Recognition**: Selenium-based Web Speech API
- **Automation**: `pyautogui`, `AppOpener`, `pywhatkit`

## 🚀 Setup and Installation

Follow these steps to get J.A.R.V.I.S. running on your local machine.

### 1. Prerequisites

- Python 3.10 or higher
- A modern web browser (like Microsoft Edge or Google Chrome)

### 2. Clone the Repository

```bash
git clone https://github.com/harsh-kakadiya1/Jarvis-Personal-ai-agent.git
cd Jarvis-Personal-ai-agent
```

### 3. Create a Virtual Environment

It's highly recommended to use a virtual environment to manage dependencies.

```bash
# Create the virtual environment
python -m venv venv

# Activate it
# On Windows
venv\Scripts\activate
# On macOS/Linux
source venv/bin/activate
```

### 4. Install Dependencies

Install all the required Python packages using the `requirements.txt` file.

```bash
pip install -r requirements.txt
```

### 5. Configure API Keys

J.A.R.V.I.S. requires several API keys to function.

- Create a file named `.env` in the root directory of the project.
- Add your API keys to this file in the following format:

```plaintext
# .env file

# For Conversational AI
GroqAPIKey=YOUR_GROQ_API_KEY

# For Decision Making
CohereAPIKey=YOUR_COHERE_API_KEY

# For Image Generation (using Gemini)
GEMINI_API_KEY=YOUR_GEMINI_API_KEY

# For Real-Time Search
GOOGLE_API_KEY=YOUR_GOOGLE_CLOUD_API_KEY
SEARCH_ENGINE_ID=YOUR_CUSTOM_SEARCH_ENGINE_ID

# --- Optional ---
# Personalization
Username=YourName
Assistantname=Jarvis
```

**Where to get the keys:**
- **Groq**: [GroqCloud Console](https://console.groq.com/keys)
- **Cohere**: [Cohere Dashboard](https://dashboard.cohere.com/api-keys)
- **Gemini**: [Google AI Studio](https://aistudio.google.com/app/apikey)
- **Google Custom Search**:
    1.  Get an API key from the [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
    2.  Create a Custom Search Engine and get its ID from the [Control Panel](https://programmablesearchengine.google.com/controlpanel/all).

### 6. Run the Application

Once everything is set up, you can start the assistant by running `Main.py`.

```bash
python Main.py
```

## 🎤 How to Use

1.  **Start the application**. The GUI will appear, and the assistant will initialize.
2.  **Click the microphone icon** or use the refresh button to activate the assistant.
3.  **Speak your command** clearly. The assistant will process your request and respond.

### Example Commands

- **General Questions**: "Hello, how are you?", "What's the capital of France?"
- **Real-Time Info**: "What are the latest news headlines?", "What's the weather in London?"
- **Automation**:
    - "Open Chrome"
    - "Open my downloads folder"
    - "Take a screenshot"
    - "Increase the volume"
- **Image Generation**: "Generate an image of a futuristic city"
- **YouTube**: "Play a song by Queen on YouTube"
