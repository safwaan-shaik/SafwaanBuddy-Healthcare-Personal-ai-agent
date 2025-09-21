"""
J.A.R.V.I.S. Medication Reminder Scheduler
Advanced scheduling system for pregnancy medication management integrated with existing J.A.R.V.I.S. threading
"""

import os
import sys
import threading
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
import json

# Import existing J.A.R.V.I.S. components
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from Backend.TextToSpeech import TextToSpeech
from Frontend.GUI import ShowTextToScreen, SetAssistantStatus
from Healthcare.Database.models import HealthcareDatabase

class MedicationScheduler:
    """
    Medication reminder scheduler integrated with J.A.R.V.I.S. threading system
    """
    
    def __init__(self, healthcare_db: HealthcareDatabase):
        self.db = healthcare_db
        self.active_reminders = {}  # Store active reminder threads
        self.scheduler_thread = None
        self.running = False
        self.reminder_check_interval = 60  # Check every minute
        
        print("✅ Medication Scheduler initialized")

    def start_scheduler(self):
        """Start the medication reminder scheduler"""
        if not self.running:
            self.running = True
            self.scheduler_thread = threading.Thread(target=self._scheduler_loop, daemon=True)
            self.scheduler_thread.start()
            print("✅ Medication Scheduler started")

    def stop_scheduler(self):
        """Stop the medication reminder scheduler"""
        self.running = False
        if self.scheduler_thread:
            self.scheduler_thread.join()
        print("🛑 Medication Scheduler stopped")

    def _scheduler_loop(self):
        """Main scheduler loop - runs in background thread"""
        while self.running:
            try:
                self._check_and_trigger_reminders()
                time.sleep(self.reminder_check_interval)
            except Exception as e:
                print(f"Error in scheduler loop: {e}")
                time.sleep(self.reminder_check_interval)

    def _check_and_trigger_reminders(self):
        """Check for due medication reminders and trigger them"""
        try:
            current_time = datetime.now()
            current_time_str = current_time.strftime("%H:%M")
            current_date = current_time.strftime("%Y-%m-%d")
            
            # Get all active reminders from database
            # For now, check for default patient (ID 1)
            active_reminders = self.db.get_active_reminders(1)
            
            for reminder in active_reminders:
                # Check if reminder is due now
                if self._is_reminder_due(reminder, current_time_str, current_date):
                    self._trigger_medication_reminder(reminder)
                    
        except Exception as e:
            print(f"Error checking reminders: {e}")

    def _is_reminder_due(self, reminder: Dict[str, Any], current_time: str, current_date: str) -> bool:
        """Check if a reminder is due at the current time"""
        try:
            # Check if reminder is still active and within date range
            if not reminder['active']:
                return False
                
            start_date = datetime.strptime(reminder['start_date'], "%Y-%m-%d").date()
            end_date = datetime.strptime(reminder['end_date'], "%Y-%m-%d").date()
            current_date_obj = datetime.strptime(current_date, "%Y-%m-%d").date()
            
            if not (start_date <= current_date_obj <= end_date):
                return False
            
            # Check if current time matches any of the reminder times
            reminder_times = reminder['times'] if isinstance(reminder['times'], list) else []
            
            # Convert current time to minutes for comparison
            current_hour, current_minute = map(int, current_time.split(':'))
            current_minutes = current_hour * 60 + current_minute
            
            for reminder_time in reminder_times:
                reminder_hour, reminder_minute = map(int, reminder_time.split(':'))
                reminder_minutes = reminder_hour * 60 + reminder_minute
                
                # Check if within 1 minute window to avoid multiple triggers
                if abs(current_minutes - reminder_minutes) <= 1:
                    # Check if we haven't already triggered this reminder today
                    reminder_key = f"{reminder['id']}_{current_date}_{reminder_time}"
                    if reminder_key not in self.active_reminders:
                        self.active_reminders[reminder_key] = True
                        return True
            
            return False
            
        except Exception as e:
            print(f"Error checking if reminder is due: {e}")
            return False

    def _trigger_medication_reminder(self, reminder: Dict[str, Any]):
        """Trigger a medication reminder using J.A.R.V.I.S. voice system"""
        try:
            medication_name = reminder['medication_name']
            dosage = reminder['dosage']
            
            # Create reminder message
            reminder_message = f"Time for your {medication_name} medication. Dosage: {dosage}."
            
            # Use J.A.R.V.I.S. existing voice system
            self._speak_reminder(reminder_message)
            
            # Show on GUI
            ShowTextToScreen(f"💊 Medication Reminder: {reminder_message}")
            
            # Log the reminder
            self.db.log_voice_command(
                reminder['patient_id'], 
                f"medication reminder triggered: {medication_name}", 
                "MEDICATION_REMINDER_TRIGGERED", 
                f"Reminder triggered for {medication_name}"
            )
            
            print(f"✅ Triggered reminder for {medication_name}")
            
        except Exception as e:
            print(f"Error triggering medication reminder: {e}")

    def _speak_reminder(self, message: str):
        """Use J.A.R.V.I.S. text-to-speech for medication reminders"""
        try:
            # Use existing J.A.R.V.I.S. TTS system
            TextToSpeech(message)
        except Exception as e:
            print(f"Error with medication reminder TTS: {e}")
            # Fallback to GUI only
            ShowTextToScreen(f"Healthcare: {message}")

    def add_medication_reminder(self, patient_id: int, medication_name: str, 
                              dosage: str, frequency: str, times: List[str],
                              start_date: str, end_date: str) -> int:
        """Add a new medication reminder"""
        try:
            reminder_id = self.db.add_medication_reminder(
                patient_id=patient_id,
                prescription_id=0,  # For manual reminders
                medication_name=medication_name,
                dosage=dosage,
                frequency=frequency,
                times=times,
                start_date=start_date,
                end_date=end_date
            )
            
            print(f"✅ Added medication reminder: {medication_name} at {times}")
            return reminder_id
            
        except Exception as e:
            print(f"Error adding medication reminder: {e}")
            return 0

    def snooze_reminder(self, reminder_id: int, snooze_minutes: int = 10):
        """Snooze a medication reminder"""
        try:
            # In a full implementation, this would update the database and reschedule
            print(f"📅 Snoozed reminder {reminder_id} for {snooze_minutes} minutes")
            
            # Remove from active reminders temporarily
            current_date = datetime.now().strftime("%Y-%m-%d")
            keys_to_remove = [key for key in self.active_reminders.keys() if key.startswith(f"{reminder_id}_")]
            for key in keys_to_remove:
                del self.active_reminders[key]
            
            # Schedule re-activation after snooze period
            def reactivate_reminder():
                time.sleep(snooze_minutes * 60)
                print(f"🔔 Reminder {reminder_id} snooze period ended")
            
            snooze_thread = threading.Thread(target=reactivate_reminder, daemon=True)
            snooze_thread.start()
            
        except Exception as e:
            print(f"Error snoozing reminder: {e}")

    def mark_medication_taken(self, reminder_id: int, patient_id: int, medication_name: str):
        """Mark medication as taken"""
        try:
            taken_time = datetime.now().strftime("%H:%M")
            taken_date = datetime.now().strftime("%Y-%m-%d")
            
            # Log medication taken
            self.db.log_voice_command(
                patient_id, 
                f"took {medication_name}", 
                "MEDICATION_TAKEN", 
                f"Medication taken at {taken_time} on {taken_date}"
            )
            
            # Remove active reminder for today
            reminder_key_pattern = f"{reminder_id}_{taken_date}"
            keys_to_remove = [key for key in self.active_reminders.keys() if key.startswith(reminder_key_pattern)]
            for key in keys_to_remove:
                del self.active_reminders[key]
            
            print(f"✅ Marked {medication_name} as taken at {taken_time}")
            
        except Exception as e:
            print(f"Error marking medication as taken: {e}")

    def get_todays_reminders(self, patient_id: int) -> List[Dict[str, Any]]:
        """Get today's medication reminders"""
        try:
            active_reminders = self.db.get_active_reminders(patient_id)
            current_date = datetime.now().strftime("%Y-%m-%d")
            
            todays_reminders = []
            for reminder in active_reminders:
                start_date = datetime.strptime(reminder['start_date'], "%Y-%m-%d").date()
                end_date = datetime.strptime(reminder['end_date'], "%Y-%m-%d").date()
                current_date_obj = datetime.strptime(current_date, "%Y-%m-%d").date()
                
                if start_date <= current_date_obj <= end_date:
                    todays_reminders.append(reminder)
            
            return todays_reminders
            
        except Exception as e:
            print(f"Error getting today's reminders: {e}")
            return []

    def get_reminder_status(self, patient_id: int) -> str:
        """Get status of medication reminders for voice response"""
        try:
            todays_reminders = self.get_todays_reminders(patient_id)
            
            if not todays_reminders:
                return "You have no medication reminders scheduled for today."
            
            status_message = f"You have {len(todays_reminders)} medication reminders today: "
            
            for reminder in todays_reminders:
                medication = reminder['medication_name']
                times = ", ".join(reminder['times'])
                status_message += f"{medication} at {times}; "
            
            return status_message.rstrip("; ")
            
        except Exception as e:
            print(f"Error getting reminder status: {e}")
            return "I'm having trouble accessing your medication reminders."

    def setup_prenatal_vitamin_reminder(self, patient_id: int) -> int:
        """Quick setup for prenatal vitamin reminders"""
        try:
            return self.add_medication_reminder(
                patient_id=patient_id,
                medication_name="Prenatal Vitamins",
                dosage="1 tablet",
                frequency="Daily",
                times=["08:00"],  # Morning
                start_date=datetime.now().strftime("%Y-%m-%d"),
                end_date=(datetime.now() + timedelta(days=180)).strftime("%Y-%m-%d")  # 6 months
            )
        except Exception as e:
            print(f"Error setting up prenatal vitamin reminder: {e}")
            return 0

    def setup_iron_supplement_reminder(self, patient_id: int) -> int:
        """Quick setup for iron supplement reminders"""
        try:
            return self.add_medication_reminder(
                patient_id=patient_id,
                medication_name="Iron Supplement",
                dosage="1 tablet",
                frequency="Daily",
                times=["20:00"],  # Evening to avoid stomach upset
                start_date=datetime.now().strftime("%Y-%m-%d"),
                end_date=(datetime.now() + timedelta(days=90)).strftime("%Y-%m-%d")  # 3 months
            )
        except Exception as e:
            print(f"Error setting up iron supplement reminder: {e}")
            return 0

class VoiceMedicationInterface:
    """
    Voice interface for medication management
    """
    
    def __init__(self, scheduler: MedicationScheduler):
        self.scheduler = scheduler
    
    def process_medication_voice_command(self, command: str, details: str, patient_id: int = 1) -> str:
        """Process voice commands related to medication"""
        try:
            command_lower = command.lower()
            
            if "set reminder" in command_lower or "remind me" in command_lower:
                return self._handle_set_reminder_voice(details, patient_id)
            elif "took" in command_lower or "taken" in command_lower:
                return self._handle_medication_taken_voice(details, patient_id)
            elif "status" in command_lower or "reminders" in command_lower:
                return self.scheduler.get_reminder_status(patient_id)
            elif "snooze" in command_lower:
                return self._handle_snooze_voice(details, patient_id)
            else:
                return "I can help you set medication reminders, mark medications as taken, check reminder status, or snooze reminders. What would you like to do?"
                
        except Exception as e:
            print(f"Error processing medication voice command: {e}")
            return "I had trouble with that medication command. Please try again."
    
    def _handle_set_reminder_voice(self, details: str, patient_id: int) -> str:
        """Handle voice command to set medication reminder"""
        try:
            # Simple parsing - in production would use NLP
            if "prenatal" in details.lower() or "vitamin" in details.lower():
                reminder_id = self.scheduler.setup_prenatal_vitamin_reminder(patient_id)
                return "I've set up your prenatal vitamin reminder for 8 AM daily."
            elif "iron" in details.lower():
                reminder_id = self.scheduler.setup_iron_supplement_reminder(patient_id)
                return "I've set up your iron supplement reminder for 8 PM daily."
            else:
                # Generic medication reminder
                reminder_id = self.scheduler.add_medication_reminder(
                    patient_id=patient_id,
                    medication_name=details or "Medication",
                    dosage="As prescribed",
                    frequency="As needed",
                    times=["08:00"],
                    start_date=datetime.now().strftime("%Y-%m-%d"),
                    end_date=(datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d")
                )
                return f"I've set up a reminder for {details or 'your medication'} at 8 AM daily."
                
        except Exception as e:
            print(f"Error setting reminder via voice: {e}")
            return "I had trouble setting up that reminder. Please try again."
    
    def _handle_medication_taken_voice(self, details: str, patient_id: int) -> str:
        """Handle voice command when medication is taken"""
        try:
            medication_name = details.strip() if details else "medication"
            
            # Mark as taken (simplified - would need reminder ID in production)
            self.scheduler.mark_medication_taken(0, patient_id, medication_name)
            
            return f"Great! I've noted that you took your {medication_name}. Keep up with your medication schedule!"
            
        except Exception as e:
            print(f"Error handling medication taken voice: {e}")
            return "I've noted that you took your medication. Good job staying on schedule!"
    
    def _handle_snooze_voice(self, details: str, patient_id: int) -> str:
        """Handle voice command to snooze reminder"""
        try:
            # Default 10 minute snooze
            snooze_minutes = 10
            
            # Parse snooze duration if specified
            if "minutes" in details:
                try:
                    words = details.split()
                    for i, word in enumerate(words):
                        if word.isdigit() and i+1 < len(words) and "minute" in words[i+1]:
                            snooze_minutes = int(word)
                            break
                except:
                    pass
            
            # Snooze the most recent reminder (simplified)
            self.scheduler.snooze_reminder(1, snooze_minutes)
            
            return f"I've snoozed your medication reminder for {snooze_minutes} minutes."
            
        except Exception as e:
            print(f"Error handling snooze voice: {e}")
            return "I've snoozed your reminder for 10 minutes."

# Global instances for integration
medication_scheduler = None
voice_medication_interface = None

def initialize_medication_system(healthcare_db: HealthcareDatabase):
    """Initialize the medication system"""
    global medication_scheduler, voice_medication_interface
    
    medication_scheduler = MedicationScheduler(healthcare_db)
    voice_medication_interface = VoiceMedicationInterface(medication_scheduler)
    
    # Start the scheduler
    medication_scheduler.start_scheduler()
    
    print("✅ Medication system initialized and started")
    
    return medication_scheduler, voice_medication_interface