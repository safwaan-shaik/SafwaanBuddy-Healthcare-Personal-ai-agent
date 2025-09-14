import asyncio
from random import randint
from PIL import Image
import requests
from dotenv import get_key
import os
from time import sleep
from google import genai
from google.genai import types

from io import BytesIO
# Configure API keys
GOOGLE_API_KEY = get_key(".env", "GOOGLE_API_KEY")
os.environ['GOOGLE_API_KEY'] = GOOGLE_API_KEY

def open_image(prompt):
    folder_path = r"Data\\generated_images"
    prompt_safe = prompt.replace(" ", "_")
    files = []
    for i in range(1,4):
        path = os.path.join(folder_path, f"{prompt_safe}{i}.png")
        if os.path.exists(path):
            files.append(path)
    if not files:
        print("[ImageGen] No images to open.")
        return
    for path in files:
        try:
            Image.open(path).show()
            sleep(0.6)
        except Exception as e:
            print(f"[ImageGen] Failed to open {path}: {e}")

def generate_gemini_images(prompt: str, num_images: int = 3):
    client = genai.Client()
    folder_path = r"Data\\generated_images"
    os.makedirs(folder_path, exist_ok=True)
    safe = prompt.replace(' ', '_')
    generated = 0
    attempt = 0
    while generated < num_images and attempt < num_images * 3:
        attempt += 1
        try:
            response = client.models.generate_content(
                model="gemini-2.0-flash-preview-image-generation",
                contents=prompt,
                config=types.GenerateContentConfig(response_modalities=['TEXT','IMAGE'])
            )
            parts = response.candidates[0].content.parts
            for part in parts:
                if getattr(part, 'inline_data', None) and part.inline_data.data:
                    generated += 1
                    filename = f"{safe}{generated}.png"
                    path = os.path.join(folder_path, filename)
                    Image.open(BytesIO(part.inline_data.data)).save(path)
                    print(f"[ImageGen] Saved {path}")
                    if generated >= num_images:
                        break
        except Exception as e:
            print(f"[ImageGen] Attempt {attempt} failed: {e}")
            sleep(1)
    if generated == 0:
        print("[ImageGen] No images generated.")

def GenerateImage(prompt: str):
    generate_gemini_images(prompt, num_images=3)
    open_image(prompt)

while True:
    try:
        print("Checking ImageGeneration.data...")
        with open(r"Frontend/Files/ImageGeneration.data", "r") as file:
            Data: str = file.read().strip()
        if "," not in Data:
            sleep(1)
            continue
        prompt, Status = Data.split(",", 1)
        if Status.strip() == "True":
            print(f"Generating images ...")
            GenerateImage(prompt=prompt.strip())
            with open(r"Frontend/Files/ImageGeneration.data", "w") as file:
                file.write("False,False")
            break
        else:
            sleep(1)
    
    except Exception as e:
        print("Error:", e)
        sleep(1)

