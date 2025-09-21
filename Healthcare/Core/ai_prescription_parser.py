"""
J.A.R.V.I.S. AI-Powered Prescription Parser
Advanced prescription parsing using OpenAI GPT for enhanced accuracy
"""

import os
import sys
import json
import re
from typing import Dict, List, Any, Optional, Tuple
from datetime import datetime
import requests

# Import existing healthcare components
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from Healthcare.Core.medical_ocr import MedicalOCR
from Healthcare.Database.models import HealthcareDatabase

# OpenAI Integration
try:
    import openai
    from dotenv import dotenv_values
    
    env_vars = dotenv_values('.env')
    openai_api_key = env_vars.get('OPENAI_API_KEY')
    
    if openai_api_key:
        openai.api_key = openai_api_key
        OPENAI_AVAILABLE = True
        print("✅ OpenAI API key configured")
    else:
        OPENAI_AVAILABLE = False
        print("⚠️ OpenAI API key not found in .env file")
        
except ImportError:
    OPENAI_AVAILABLE = False
    print("⚠️ OpenAI library not available. Install with: pip install openai")

class AIPrescriptionParser:
    """
    AI-powered prescription parser using OpenAI GPT for enhanced medical text processing
    """
    
    def __init__(self):
        self.medical_ocr = MedicalOCR()
        self.healthcare_db = HealthcareDatabase()
        
        # Medical knowledge prompts
        self.system_prompt = """You are a medical prescription parsing assistant. 
        Your task is to extract structured medication information from prescription text.
        
        Always respond with valid JSON in this exact format:
        {
            "medications": [
                {
                    "name": "medication name",
                    "strength": "dosage strength (e.g., 500mg)",
                    "form": "tablet/capsule/liquid/etc",
                    "frequency": "how often (e.g., twice daily)",
                    "duration": "how long (e.g., 7 days)",
                    "instructions": "special instructions",
                    "quantity": "number prescribed",
                    "refills": "number of refills"
                }
            ],
            "prescriber": "doctor name if found",
            "date": "prescription date if found",
            "patient": "patient name if found"
        }
        
        If information is missing, use "Not specified" for that field.
        Focus on pregnancy-safe medications and flag any potentially concerning drugs."""
        
        # Pregnancy medication safety database
        self.pregnancy_categories = {
            'safe': [
                'prenatal vitamin', 'folic acid', 'iron', 'calcium', 'vitamin d',
                'acetaminophen', 'paracetamol', 'methyldopa', 'labetalol'
            ],
            'caution': [
                'ibuprofen', 'aspirin', 'naproxen', 'codeine', 'hydrocodone',
                'prednisone', 'metformin', 'insulin'
            ],
            'avoid': [
                'warfarin', 'ace inhibitors', 'angiotensin', 'isotretinoin',
                'methotrexate', 'lithium', 'phenytoin', 'valproic acid'
            ]
        }
        
        print("✅ AI Prescription Parser initialized")
    
    def parse_prescription_with_ai(self, image_path: str) -> Dict[str, Any]:
        """
        Parse prescription using AI enhancement
        """
        try:
            # First, extract text using OCR
            ocr_text = self.medical_ocr.extract_text_from_image(image_path)
            
            if "Error" in ocr_text or len(ocr_text.strip()) < 10:
                return {
                    'success': False,
                    'error': 'Could not extract readable text from prescription',
                    'raw_ocr_text': ocr_text
                }
            
            # Use AI to parse the extracted text
            if OPENAI_AVAILABLE:
                ai_parsed_data = self._parse_with_openai(ocr_text)
            else:
                # Fallback to rule-based parsing
                ai_parsed_data = self._parse_with_rules(ocr_text)
            
            if not ai_parsed_data.get('medications'):
                return {
                    'success': False,
                    'error': 'No medications found in prescription',
                    'raw_ocr_text': ocr_text,
                    'ai_response': ai_parsed_data
                }
            
            # Enhance with pregnancy safety analysis
            enhanced_data = self._add_pregnancy_safety_analysis(ai_parsed_data)
            
            # Store in database
            prescription_id = self.healthcare_db.add_prescription(
                patient_id=1,  # Default patient
                image_path=image_path,
                ocr_text=ocr_text,
                parsed_medications=enhanced_data
            )
            
            # Generate medication reminders
            reminder_ids = self._create_medication_reminders(enhanced_data, prescription_id)
            
            return {
                'success': True,
                'prescription_id': prescription_id,
                'raw_ocr_text': ocr_text,
                'parsed_data': enhanced_data,
                'medication_count': len(enhanced_data['medications']),
                'reminder_ids': reminder_ids,
                'safety_alerts': enhanced_data.get('safety_alerts', [])
            }
            
        except Exception as e:
            print(f"Error in AI prescription parsing: {e}")
            return {
                'success': False,
                'error': f'AI parsing failed: {str(e)}',
                'raw_ocr_text': ocr_text if 'ocr_text' in locals() else 'N/A'
            }
    
    def _parse_with_openai(self, ocr_text: str) -> Dict[str, Any]:
        """
        Parse prescription text using OpenAI GPT
        """
        try:
            user_prompt = f"""Parse this prescription text and extract medication information:

{ocr_text}

Focus on pregnancy-related medications. If any medications seem inappropriate for pregnancy, note this in the response."""

            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": self.system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=0.1,  # Low temperature for consistency
                max_tokens=1000
            )
            
            ai_response = response.choices[0].message.content.strip()
            
            # Parse JSON response
            try:
                parsed_data = json.loads(ai_response)
                return parsed_data
            except json.JSONDecodeError:
                # Try to extract JSON from response if it's wrapped in text
                json_match = re.search(r'\{.*\}', ai_response, re.DOTALL)
                if json_match:
                    return json.loads(json_match.group())
                else:
                    raise ValueError("Could not parse JSON from AI response")
                    
        except Exception as e:
            print(f"OpenAI parsing error: {e}")
            # Fallback to rule-based parsing
            return self._parse_with_rules(ocr_text)
    
    def _parse_with_rules(self, ocr_text: str) -> Dict[str, Any]:
        """
        Fallback rule-based parsing when AI is not available
        """
        try:
            medications = []
            lines = ocr_text.split('\n')
            
            current_medication = {}
            
            for line in lines:
                line = line.strip()
                if not line:
                    continue
                
                # Look for medication names (usually capitalized)
                if re.match(r'^[A-Z][a-z]+', line):
                    # Save previous medication
                    if current_medication.get('name'):
                        medications.append(current_medication.copy())
                    
                    # Start new medication
                    current_medication = {
                        'name': self._extract_medication_name(line),
                        'strength': self._extract_strength(line),
                        'form': self._extract_form(line),
                        'frequency': self._extract_frequency(line),
                        'duration': self._extract_duration(line),
                        'instructions': self._extract_instructions(line),
                        'quantity': self._extract_quantity(line),
                        'refills': 'Not specified'
                    }
                else:
                    # Update current medication with additional info
                    if current_medication:
                        if not current_medication.get('strength'):
                            current_medication['strength'] = self._extract_strength(line) or 'Not specified'
                        if not current_medication.get('frequency'):
                            current_medication['frequency'] = self._extract_frequency(line) or 'Not specified'
                        if not current_medication.get('duration'):
                            current_medication['duration'] = self._extract_duration(line) or 'Not specified'
            
            # Add last medication
            if current_medication.get('name'):
                medications.append(current_medication)
            
            return {
                'medications': medications,
                'prescriber': self._extract_prescriber(ocr_text),
                'date': self._extract_date(ocr_text),
                'patient': self._extract_patient_name(ocr_text)
            }
            
        except Exception as e:
            print(f"Rule-based parsing error: {e}")
            return {'medications': [], 'error': str(e)}
    
    def _extract_medication_name(self, text: str) -> str:
        """Extract medication name from text"""
        # Look for capitalized words that could be medication names
        words = text.split()
        for word in words:
            if len(word) > 3 and word[0].isupper():
                # Clean the word
                clean_word = re.sub(r'[^a-zA-Z]', '', word)
                if len(clean_word) > 3:
                    return clean_word
        return "Unknown Medication"
    
    def _extract_strength(self, text: str) -> Optional[str]:
        """Extract medication strength"""
        strength_pattern = r'(\d+(?:\.\d+)?)\s*(mg|g|ml|mcg|units?)'
        match = re.search(strength_pattern, text, re.IGNORECASE)
        return match.group(0) if match else None
    
    def _extract_form(self, text: str) -> str:
        """Extract medication form"""
        forms = ['tablet', 'capsule', 'liquid', 'injection', 'cream', 'drops']
        text_lower = text.lower()
        for form in forms:
            if form in text_lower:
                return form
        return 'tablet'  # Default
    
    def _extract_frequency(self, text: str) -> Optional[str]:
        """Extract frequency information"""
        freq_patterns = [
            r'(once|twice|thrice|\d+\s*times?)\s*(daily|per day|a day)',
            r'every\s*(\d+)\s*hours?',
            r'(morning|evening|night)',
            r'(bid|tid|qid)',  # Medical abbreviations
        ]
        
        for pattern in freq_patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                return match.group(0)
        return None
    
    def _extract_duration(self, text: str) -> Optional[str]:
        """Extract duration information"""
        duration_pattern = r'for\s*(\d+)\s*(days?|weeks?|months?)'
        match = re.search(duration_pattern, text, re.IGNORECASE)
        return match.group(0) if match else None
    
    def _extract_instructions(self, text: str) -> str:
        """Extract special instructions"""
        instruction_patterns = [
            r'take with (food|water|meals)',
            r'(before|after) (meals|food|bedtime)',
            r'on empty stomach',
            r'do not crush'
        ]
        
        instructions = []
        for pattern in instruction_patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                instructions.append(match.group(0))
        
        return '; '.join(instructions) if instructions else 'Take as directed'
    
    def _extract_quantity(self, text: str) -> str:
        """Extract quantity prescribed"""
        qty_pattern = r'(?:#|qty|quantity)\s*(\d+)'
        match = re.search(qty_pattern, text, re.IGNORECASE)
        return match.group(1) if match else 'Not specified'
    
    def _extract_prescriber(self, text: str) -> str:
        """Extract prescriber name"""
        # Look for Dr. or MD patterns
        dr_pattern = r'Dr\.?\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)'
        match = re.search(dr_pattern, text)
        if match:
            return f"Dr. {match.group(1)}"
        
        md_pattern = r'([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*),?\s*M\.?D\.?'
        match = re.search(md_pattern, text)
        if match:
            return f"Dr. {match.group(1)}"
        
        return 'Not specified'
    
    def _extract_date(self, text: str) -> str:
        """Extract prescription date"""
        date_patterns = [
            r'(\d{1,2}/\d{1,2}/\d{4})',
            r'(\d{1,2}-\d{1,2}-\d{4})',
            r'(\d{4}-\d{1,2}-\d{1,2})',
        ]
        
        for pattern in date_patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1)
        
        return datetime.now().strftime('%Y-%m-%d')
    
    def _extract_patient_name(self, text: str) -> str:
        """Extract patient name"""
        # Look for patient name patterns
        patient_patterns = [
            r'Patient:\s*([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)',
            r'Name:\s*([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)',
        ]
        
        for pattern in patient_patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1)
        
        return 'Not specified'
    
    def _add_pregnancy_safety_analysis(self, parsed_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Add pregnancy safety analysis to parsed medications
        """
        enhanced_data = parsed_data.copy()
        safety_alerts = []
        
        for medication in enhanced_data.get('medications', []):
            med_name_lower = medication.get('name', '').lower()
            safety_category = 'unknown'
            
            # Check pregnancy safety
            for category, med_list in self.pregnancy_categories.items():
                if any(safe_med in med_name_lower for safe_med in med_list):
                    safety_category = category
                    break
            
            medication['pregnancy_safety'] = safety_category
            
            # Generate safety alerts
            if safety_category == 'avoid':
                safety_alerts.append({
                    'medication': medication['name'],
                    'level': 'high',
                    'message': f"{medication['name']} may not be safe during pregnancy. Consult your doctor immediately."
                })
            elif safety_category == 'caution':
                safety_alerts.append({
                    'medication': medication['name'],
                    'level': 'medium',
                    'message': f"{medication['name']} requires caution during pregnancy. Discuss with your healthcare provider."
                })
            elif safety_category == 'safe':
                medication['pregnancy_note'] = 'Generally considered safe during pregnancy'
        
        enhanced_data['safety_alerts'] = safety_alerts
        return enhanced_data
    
    def _create_medication_reminders(self, parsed_data: Dict[str, Any], prescription_id: int) -> List[int]:
        """
        Create medication reminders from parsed prescription data
        """
        reminder_ids = []
        
        try:
            for medication in parsed_data.get('medications', []):
                # Parse frequency to determine reminder times
                frequency = medication.get('frequency', '').lower()
                times = self._frequency_to_times(frequency)
                
                if times:
                    reminder_id = self.healthcare_db.add_medication_reminder(
                        patient_id=1,
                        prescription_id=prescription_id,
                        medication_name=medication.get('name', 'Unknown'),
                        dosage=f"{medication.get('strength', '')} {medication.get('form', '')}".strip(),
                        frequency=medication.get('frequency', 'As prescribed'),
                        times=times,
                        start_date=datetime.now().strftime('%Y-%m-%d'),
                        end_date=self._calculate_end_date(medication.get('duration', '30 days'))
                    )
                    reminder_ids.append(reminder_id)
            
        except Exception as e:
            print(f"Error creating reminders: {e}")
        
        return reminder_ids
    
    def _frequency_to_times(self, frequency: str) -> List[str]:
        """
        Convert frequency text to specific reminder times
        """
        if not frequency:
            return ['08:00']  # Default morning time
        
        freq_lower = frequency.lower()
        
        if 'once' in freq_lower or '1' in freq_lower:
            return ['08:00']  # Morning
        elif 'twice' in freq_lower or '2' in freq_lower or 'bid' in freq_lower:
            return ['08:00', '20:00']  # Morning and evening
        elif 'thrice' in freq_lower or '3' in freq_lower or 'tid' in freq_lower:
            return ['08:00', '14:00', '20:00']  # Morning, afternoon, evening
        elif '4' in freq_lower or 'qid' in freq_lower:
            return ['08:00', '12:00', '16:00', '20:00']  # Every 6 hours
        elif 'morning' in freq_lower:
            return ['08:00']
        elif 'evening' in freq_lower or 'night' in freq_lower:
            return ['20:00']
        else:
            return ['08:00']  # Default
    
    def _calculate_end_date(self, duration: str) -> str:
        """
        Calculate end date from duration string
        """
        try:
            if not duration or 'not specified' in duration.lower():
                # Default to 30 days
                end_date = datetime.now() + timedelta(days=30)
                return end_date.strftime('%Y-%m-%d')
            
            # Extract number and unit
            match = re.search(r'(\d+)\s*(days?|weeks?|months?)', duration.lower())
            if match:
                number = int(match.group(1))
                unit = match.group(2)
                
                if 'day' in unit:
                    end_date = datetime.now() + timedelta(days=number)
                elif 'week' in unit:
                    end_date = datetime.now() + timedelta(weeks=number)
                elif 'month' in unit:
                    end_date = datetime.now() + timedelta(days=number * 30)
                else:
                    end_date = datetime.now() + timedelta(days=30)
                
                return end_date.strftime('%Y-%m-%d')
            else:
                # Default to 30 days
                end_date = datetime.now() + timedelta(days=30)
                return end_date.strftime('%Y-%m-%d')
                
        except Exception as e:
            print(f"Error calculating end date: {e}")
            end_date = datetime.now() + timedelta(days=30)
            return end_date.strftime('%Y-%m-%d')

# Global instance for integration
ai_prescription_parser = AIPrescriptionParser()

# Utility functions
def parse_prescription_with_ai(image_path: str) -> Dict[str, Any]:
    """Parse prescription using AI enhancement"""
    return ai_prescription_parser.parse_prescription_with_ai(image_path)

def analyze_medication_safety(medication_name: str) -> Dict[str, Any]:
    """Analyze medication safety for pregnancy"""
    parser = AIPrescriptionParser()
    med_name_lower = medication_name.lower()
    
    for category, med_list in parser.pregnancy_categories.items():
        if any(safe_med in med_name_lower for safe_med in med_list):
            return {
                'medication': medication_name,
                'safety_category': category,
                'safe_for_pregnancy': category == 'safe',
                'requires_caution': category == 'caution',
                'should_avoid': category == 'avoid'
            }
    
    return {
        'medication': medication_name,
        'safety_category': 'unknown',
        'safe_for_pregnancy': False,
        'requires_caution': True,
        'should_avoid': False
    }