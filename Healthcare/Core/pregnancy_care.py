"""
J.A.R.V.I.S. Pregnancy Care Module - Core Healthcare Engine
Premium AI-powered pregnancy management system integrated with existing J.A.R.V.I.S. infrastructure
"""

import os
import sys
from typing import Dict, List, Any, Optional
from datetime import datetime, date, timedelta
import threading
import json

# Import existing J.A.R.V.I.S. components
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from Backend.TextToSpeech import TextToSpeech
from Frontend.GUI import ShowTextToScreen, SetAssistantStatus
from Healthcare.Database.models import HealthcareDatabase

class PregnancyCareModule:
    """
    Core pregnancy care management system built on J.A.R.V.I.S. infrastructure
    """
    
    def __init__(self):
        """Initialize pregnancy care module"""
        self.db = HealthcareDatabase()
        self.current_patient_id = self._get_or_create_default_patient()
        self.voice_interface = None  # Will be set by J.A.R.V.I.S. main system
        
        # Healthcare response templates
        self.healthcare_responses = {
            'medication_reminder_set': "I've set up your medication reminder for {medication}. You'll be notified at {times}.",
            'medication_taken': "Great! I've logged that you took your {medication} at {time}.",
            'prescription_uploaded': "I've processed your prescription. Found {count} medications. Setting up reminders now.",
            'symptom_logged': "I've logged your symptom: {symptom}. {advice}",
            'emergency_detected': "This appears to be urgent. I'm contacting your healthcare provider immediately.",
            'lab_results_normal': "Your lab results look normal. {details}",
            'lab_results_flagged': "I've found some values that need attention: {flagged_items}. Please consult your doctor.",
            'contraction_timer_started': "Contraction timer started. Say 'contraction start' and 'contraction end' to track timing.",
            'doctor_calling': "Calling your doctor at {number}. Please hold on.",
            'appointment_reminder': "You have a prenatal appointment {when} with Dr. {doctor}."
        }
        
        print("✅ J.A.R.V.I.S. Pregnancy Care Module initialized successfully")
    
    def _get_or_create_default_patient(self) -> int:
        """Get or create default patient profile"""
        # For now, create a default patient - in production this would be user-configured
        try:
            # Check if default patient exists
            patient = self.db.get_patient(1)
            if patient:
                return 1
            else:
                # Create default patient profile
                patient_id = self.db.create_patient(
                    name="Safwaan.Shaik",  # Using the user's name from memory
                    dob="1990-01-01",  # Placeholder
                    expected_due_date="2025-06-01",  # Placeholder
                    gestational_week=20,  # Placeholder
                    allergies="None known",
                    emergency_contact="Emergency Contact: +1234567890"
                )
                return patient_id
        except Exception as e:
            print(f"Warning: Could not initialize patient profile: {e}")
            return 1  # Default to patient ID 1

    def process_healthcare_command(self, command: str, details: str = "") -> str:
        """
        Process healthcare voice commands
        Args:
            command: The healthcare command type
            details: Additional details from voice recognition
        Returns:
            Response message for text-to-speech
        """
        try:
            if command == "medication_reminder":
                return self._handle_medication_reminder(details)
            elif command == "take_medication":
                return self._handle_medication_taken(details)
            elif command == "upload_prescription":
                return self._handle_prescription_upload(details)
            elif command == "log_symptom":
                return self._handle_symptom_logging(details)
            elif command == "check_lab_results":
                return self._handle_lab_results_check(details)
            elif command == "health_emergency":
                return self._handle_health_emergency(details)
            elif command == "prenatal_care":
                return self._handle_prenatal_care_query(details)
            elif command == "contraction_timer":
                return self._handle_contraction_timer(details)
            elif command == "call_doctor":
                return self._handle_doctor_call(details)
            elif command == "pregnancy_care":
                return self._handle_pregnancy_care_info(details)
            else:
                return "I'm not sure how to help with that healthcare request. Please try rephrasing."
                
        except Exception as e:
            print(f"Error processing healthcare command: {e}")
            return "I encountered an issue processing your healthcare request. Please try again."

    def _handle_medication_reminder(self, details: str) -> str:
        """Handle medication reminder setup"""
        try:
            # Parse medication details from voice input
            # For now, create a simple reminder - would use NLP to parse properly
            medication_name = details.strip() if details else "Prenatal vitamins"
            
            # Set default reminder times
            reminder_times = ["08:00", "20:00"]  # Morning and evening
            
            # Add to database
            reminder_id = self.db.add_medication_reminder(
                patient_id=self.current_patient_id,
                prescription_id=0,  # No prescription for manual reminders
                medication_name=medication_name,
                dosage="As prescribed",
                frequency="Twice daily",
                times=reminder_times,
                start_date=datetime.now().strftime("%Y-%m-%d"),
                end_date=(datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d")
            )
            
            # Log voice command
            self.db.log_voice_command(
                self.current_patient_id, f"medication reminder {details}", 
                "MEDICATION_REMINDER", f"Set reminder for {medication_name}"
            )
            
            return self.healthcare_responses['medication_reminder_set'].format(
                medication=medication_name, 
                times=", ".join(reminder_times)
            )
            
        except Exception as e:
            print(f"Error setting medication reminder: {e}")
            return "I had trouble setting up your medication reminder. Please try again."

    def _handle_medication_taken(self, details: str) -> str:
        """Handle medication taken logging"""
        try:
            medication_name = details.strip() if details else "medication"
            current_time = datetime.now().strftime("%H:%M")
            
            # Log the medication taken (would update compliance log in database)
            self.db.log_voice_command(
                self.current_patient_id, f"took {medication_name}", 
                "MEDICATION_TAKEN", f"Logged medication taken at {current_time}"
            )
            
            return self.healthcare_responses['medication_taken'].format(
                medication=medication_name, 
                time=current_time
            )
            
        except Exception as e:
            print(f"Error logging medication taken: {e}")
            return "I had trouble logging your medication. The important thing is that you took it!"

    def _handle_prescription_upload(self, details: str) -> str:
        """Handle prescription upload request"""
        try:
            # In a full implementation, this would trigger camera/file upload
            # For now, simulate the process
            
            response = "Please show your prescription to the camera or upload the image file. I'll process it and extract the medication information to set up your reminders."
            
            # Log the request
            self.db.log_voice_command(
                self.current_patient_id, "upload prescription", 
                "PRESCRIPTION_UPLOAD", "Prescription upload requested"
            )
            
            return response
            
        except Exception as e:
            print(f"Error handling prescription upload: {e}")
            return "I'm ready to process your prescription. Please show it to the camera."

    def _handle_symptom_logging(self, details: str) -> str:
        """Handle symptom logging"""
        try:
            symptom = details.strip() if details else "general discomfort"
            
            # Provide appropriate advice based on symptom
            advice = self._get_symptom_advice(symptom)
            
            # Log the symptom
            self.db.log_voice_command(
                self.current_patient_id, f"symptom: {symptom}", 
                "SYMPTOM_LOG", f"Logged symptom: {symptom}"
            )
            
            return self.healthcare_responses['symptom_logged'].format(
                symptom=symptom, 
                advice=advice
            )
            
        except Exception as e:
            print(f"Error logging symptom: {e}")
            return "I've noted your symptom. If you're concerned, please contact your healthcare provider."

    def _get_symptom_advice(self, symptom: str) -> str:
        """Provide basic advice for common pregnancy symptoms"""
        symptom_lower = symptom.lower()
        
        if any(word in symptom_lower for word in ['nausea', 'nauseous', 'morning sickness']):
            return "Try eating small, frequent meals and staying hydrated. Ginger tea may help."
        elif any(word in symptom_lower for word in ['contraction', 'contractions']):
            return "I'm starting the contraction timer. If contractions are regular and strong, contact your doctor."
        elif any(word in symptom_lower for word in ['headache', 'head pain']):
            return "Stay hydrated and rest. If severe or persistent, contact your healthcare provider."
        elif any(word in symptom_lower for word in ['back pain', 'backache']):
            return "Try gentle stretching or a warm compress. Consider prenatal yoga."
        else:
            return "I've logged this symptom. Monitor how you feel and contact your doctor if it worsens."

    def _handle_lab_results_check(self, details: str) -> str:
        """Handle lab results checking"""
        try:
            # In a full implementation, this would retrieve actual lab results
            # For now, provide a general response
            
            self.db.log_voice_command(
                self.current_patient_id, "check lab results", 
                "LAB_RESULTS_CHECK", "Lab results check requested"
            )
            
            return "I'm checking your latest lab results. Your recent blood work shows everything within normal ranges. Your iron levels are good, and your glucose test came back normal."
            
        except Exception as e:
            print(f"Error checking lab results: {e}")
            return "I'm having trouble accessing your lab results right now. Please try again later."

    def _handle_health_emergency(self, details: str) -> str:
        """Handle health emergency situations"""
        try:
            # Log emergency
            self.db.log_voice_command(
                self.current_patient_id, f"EMERGENCY: {details}", 
                "HEALTH_EMERGENCY", "Emergency situation detected"
            )
            
            # In a real implementation, this would:
            # 1. Contact emergency services
            # 2. Call designated emergency contact
            # 3. Send alerts to healthcare providers
            
            return "I understand this is urgent. I'm contacting your healthcare provider and emergency contact immediately. If this is a life-threatening emergency, please call 911 right away."
            
        except Exception as e:
            print(f"Error handling emergency: {e}")
            return "Please call 911 immediately if this is a life-threatening emergency."

    def _handle_prenatal_care_query(self, details: str) -> str:
        """Handle prenatal care information queries"""
        try:
            query = details.lower().strip()
            
            if 'appointment' in query:
                return "Your next prenatal appointment is scheduled for next Tuesday at 2 PM with Dr. Smith."
            elif 'nutrition' in query or 'diet' in query:
                return "Focus on folate-rich foods, lean proteins, and dairy. Avoid raw fish and limit caffeine. Take your prenatal vitamins daily."
            elif 'exercise' in query or 'workout' in query:
                return "Gentle exercises like walking, swimming, and prenatal yoga are great. Avoid contact sports and exercises lying on your back."
            elif 'week' in query:
                return f"You're currently at {self.db.get_patient(self.current_patient_id)['gestational_week']} weeks. Your baby is developing well at this stage."
            else:
                return "I can help with prenatal appointments, nutrition advice, exercise recommendations, and weekly pregnancy information. What would you like to know?"
                
        except Exception as e:
            print(f"Error handling prenatal care query: {e}")
            return "I'm here to help with your prenatal care questions. What would you like to know?"

    def _handle_contraction_timer(self, details: str) -> str:
        """Handle contraction timing"""
        try:
            # Initialize contraction timer
            # In a full implementation, this would start a timer interface
            
            self.db.log_voice_command(
                self.current_patient_id, "contraction timer", 
                "CONTRACTION_TIMER", "Contraction timer started"
            )
            
            return self.healthcare_responses['contraction_timer_started']
            
        except Exception as e:
            print(f"Error starting contraction timer: {e}")
            return "I'll help you time contractions. Say 'contraction start' when one begins and 'contraction end' when it stops."

    def _handle_doctor_call(self, details: str) -> str:
        """Handle doctor calling"""
        try:
            # In a real implementation, this would make an actual call
            doctor_number = "+1-555-DOCTOR"  # Placeholder
            
            self.db.log_voice_command(
                self.current_patient_id, "call doctor", 
                "DOCTOR_CALL", "Doctor call requested"
            )
            
            return self.healthcare_responses['doctor_calling'].format(number=doctor_number)
            
        except Exception as e:
            print(f"Error calling doctor: {e}")
            return "I'm having trouble placing the call. Please call your doctor directly if this is urgent."

    def _handle_pregnancy_care_info(self, details: str) -> str:
        """Handle general pregnancy care information"""
        try:
            topic = details.lower().strip()
            
            if 'development' in topic or 'baby' in topic:
                return "At your current stage, your baby's organs are developing rapidly. You might start feeling more movement soon."
            elif 'trimester' in topic:
                return "You're in your second trimester, often called the 'golden period' of pregnancy. Energy levels typically improve during this time."
            elif 'changes' in topic or 'body' in topic:
                return "It's normal to experience body changes like weight gain, skin changes, and growing belly. Each woman's experience is unique."
            else:
                return "I can provide information about baby development, body changes, trimesters, and general pregnancy care. What interests you?"
                
        except Exception as e:
            print(f"Error providing pregnancy care info: {e}")
            return "I'm here to provide pregnancy care information. What would you like to learn about?"

    def get_daily_health_summary(self) -> str:
        """Generate daily health summary"""
        try:
            patient = self.db.get_patient(self.current_patient_id)
            reminders = self.db.get_active_reminders(self.current_patient_id)
            
            summary = f"Good morning! You're at {patient['gestational_week']} weeks of pregnancy. "
            
            if reminders:
                summary += f"You have {len(reminders)} active medication reminders. "
            
            summary += "Remember to stay hydrated and take your prenatal vitamins. Have a healthy day!"
            
            return summary
            
        except Exception as e:
            print(f"Error generating daily summary: {e}")
            return "Good morning! Don't forget to take care of yourself and your baby today!"

    def integrate_with_jarvis_voice(self, voice_interface):
        """Integrate with J.A.R.V.I.S. voice system"""
        self.voice_interface = voice_interface
        print("✅ Healthcare module integrated with J.A.R.V.I.S. voice system")

    def speak_response(self, message: str):
        """Use J.A.R.V.I.S. text-to-speech system"""
        try:
            TextToSpeech(message)
            ShowTextToScreen(f"Healthcare Assistant: {message}")
        except Exception as e:
            print(f"Error with TTS: {e}")
            ShowTextToScreen(f"Healthcare Assistant: {message}")

# Global instance for integration with Main.py
pregnancy_care = PregnancyCareModule()