"""
Healthcare Database Models
SQLAlchemy models for pregnancy care data with encryption support
"""

import sqlite3
import json
import os
from datetime import datetime, date
from typing import Optional, List, Dict, Any
from Healthcare.Database.encryption import HealthcareEncryption, HealthcareAuditLogger

class HealthcareDatabase:
    """
    Healthcare database manager with encryption support
    """
    
    def __init__(self, db_path: str = "Healthcare/Database/healthcare.db"):
        self.db_path = db_path
        self.encryption = HealthcareEncryption()
        self.audit_logger = HealthcareAuditLogger(self.encryption)
        self._ensure_database_exists()
    
    def _ensure_database_exists(self):
        """Create database and tables if they don't exist"""
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            
            # Create patients table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS patients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    dob TEXT,
                    expected_due_date TEXT,
                    gestational_week INTEGER,
                    allergies TEXT,
                    emergency_contact TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            # Create prescriptions table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS prescriptions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id INTEGER REFERENCES patients(id),
                    image_path TEXT,
                    ocr_text TEXT,
                    parsed_medications TEXT,
                    verification_status TEXT DEFAULT 'pending',
                    clinician_verified BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            # Create medication reminders table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS medication_reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id INTEGER REFERENCES patients(id),
                    prescription_id INTEGER REFERENCES prescriptions(id),
                    medication_name TEXT,
                    dosage TEXT,
                    frequency TEXT,
                    times TEXT,
                    start_date TEXT,
                    end_date TEXT,
                    active BOOLEAN DEFAULT TRUE,
                    snooze_count INTEGER DEFAULT 0,
                    compliance_log TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            # Create lab results table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS lab_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id INTEGER REFERENCES patients(id),
                    test_date TEXT,
                    test_type TEXT,
                    results TEXT,
                    flagged_values TEXT,
                    urgency_level TEXT DEFAULT 'normal',
                    clinician_notified BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            # Create voice commands log table
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS healthcare_voice_commands (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_id INTEGER,
                    command_text TEXT,
                    intent_classification TEXT,
                    response_generated TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            conn.commit()

    def create_patient(self, name: str, dob: str, expected_due_date: str, 
                      gestational_week: int, allergies: str = "", 
                      emergency_contact: str = "") -> int:
        """Create new patient record with encryption"""
        
        # Encrypt sensitive data
        encrypted_name = self.encryption.encrypt_data(name)
        encrypted_dob = self.encryption.encrypt_data(dob)
        encrypted_allergies = self.encryption.encrypt_data(allergies) if allergies else ""
        encrypted_emergency_contact = self.encryption.encrypt_data(emergency_contact) if emergency_contact else ""
        
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO patients (name, dob, expected_due_date, gestational_week, 
                                    allergies, emergency_contact)
                VALUES (?, ?, ?, ?, ?, ?)
            ''', (encrypted_name, encrypted_dob, expected_due_date, gestational_week,
                  encrypted_allergies, encrypted_emergency_contact))
            
            patient_id = cursor.lastrowid
            conn.commit()
            
            # Log audit trail
            self.audit_logger.log_medical_interaction(
                "CREATE_PATIENT", str(patient_id), 
                {"action": "Patient record created", "gestational_week": gestational_week}
            )
            
            return patient_id

    def get_patient(self, patient_id: int) -> Optional[Dict[str, Any]]:
        """Get patient record with decryption"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('SELECT * FROM patients WHERE id = ?', (patient_id,))
            row = cursor.fetchone()
            
            if row:
                # Decrypt sensitive fields
                try:
                    decrypted_name = self.encryption.decrypt_data(row[1]) if row[1] else ""
                    decrypted_dob = self.encryption.decrypt_data(row[2]) if row[2] else ""
                    decrypted_allergies = self.encryption.decrypt_data(row[5]) if row[5] else ""
                    decrypted_emergency_contact = self.encryption.decrypt_data(row[6]) if row[6] else ""
                except Exception as e:
                    print(f"Warning: Could not decrypt patient data: {e}")
                    decrypted_name = "ENCRYPTED_DATA"
                    decrypted_dob = "ENCRYPTED_DATA"
                    decrypted_allergies = "ENCRYPTED_DATA"
                    decrypted_emergency_contact = "ENCRYPTED_DATA"
                
                return {
                    'id': row[0],
                    'name': decrypted_name,
                    'dob': decrypted_dob,
                    'expected_due_date': row[3],
                    'gestational_week': row[4],
                    'allergies': decrypted_allergies,
                    'emergency_contact': decrypted_emergency_contact,
                    'created_at': row[7]
                }
        return None

    def add_prescription(self, patient_id: int, image_path: str, ocr_text: str, 
                        parsed_medications: Dict[str, Any]) -> int:
        """Add prescription record with encryption"""
        
        # Encrypt sensitive data
        encrypted_image_path = self.encryption.encrypt_data(image_path)
        encrypted_ocr_text = self.encryption.encrypt_data(ocr_text)
        encrypted_medications = self.encryption.encrypt_json(parsed_medications)
        
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO prescriptions (patient_id, image_path, ocr_text, parsed_medications)
                VALUES (?, ?, ?, ?)
            ''', (patient_id, encrypted_image_path, encrypted_ocr_text, encrypted_medications))
            
            prescription_id = cursor.lastrowid
            conn.commit()
            
            # Log audit trail
            self.audit_logger.log_medical_interaction(
                "ADD_PRESCRIPTION", str(patient_id),
                {"prescription_id": prescription_id, "medication_count": len(parsed_medications)}
            )
            
            return prescription_id

    def add_medication_reminder(self, patient_id: int, prescription_id: int,
                               medication_name: str, dosage: str, frequency: str,
                               times: List[str], start_date: str, end_date: str) -> int:
        """Add medication reminder"""
        
        times_json = json.dumps(times)
        
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO medication_reminders 
                (patient_id, prescription_id, medication_name, dosage, frequency, 
                 times, start_date, end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (patient_id, prescription_id, medication_name, dosage, frequency,
                  times_json, start_date, end_date))
            
            reminder_id = cursor.lastrowid
            conn.commit()
            
            # Log audit trail
            self.audit_logger.log_medical_interaction(
                "ADD_MEDICATION_REMINDER", str(patient_id),
                {"reminder_id": reminder_id, "medication": medication_name, "frequency": frequency}
            )
            
            return reminder_id

    def get_active_reminders(self, patient_id: int) -> List[Dict[str, Any]]:
        """Get active medication reminders for patient"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT * FROM medication_reminders 
                WHERE patient_id = ? AND active = TRUE
                ORDER BY created_at DESC
            ''', (patient_id,))
            
            reminders = []
            for row in cursor.fetchall():
                reminders.append({
                    'id': row[0],
                    'patient_id': row[1],
                    'prescription_id': row[2],
                    'medication_name': row[3],
                    'dosage': row[4],
                    'frequency': row[5],
                    'times': json.loads(row[6]) if row[6] else [],
                    'start_date': row[7],
                    'end_date': row[8],
                    'active': row[9],
                    'snooze_count': row[10],
                    'compliance_log': row[11],
                    'created_at': row[12]
                })
            
            return reminders

    def add_lab_result(self, patient_id: int, test_date: str, test_type: str,
                      results: Dict[str, Any], flagged_values: Dict[str, Any] = None,
                      urgency_level: str = "normal") -> int:
        """Add lab result with encryption"""
        
        # Encrypt sensitive data
        encrypted_results = self.encryption.encrypt_json(results)
        encrypted_flagged = self.encryption.encrypt_json(flagged_values) if flagged_values else ""
        
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO lab_results (patient_id, test_date, test_type, results, 
                                       flagged_values, urgency_level)
                VALUES (?, ?, ?, ?, ?, ?)
            ''', (patient_id, test_date, test_type, encrypted_results, 
                  encrypted_flagged, urgency_level))
            
            result_id = cursor.lastrowid
            conn.commit()
            
            # Log audit trail
            self.audit_logger.log_medical_interaction(
                "ADD_LAB_RESULT", str(patient_id),
                {"result_id": result_id, "test_type": test_type, "urgency": urgency_level}
            )
            
            return result_id

    def log_voice_command(self, patient_id: int, command_text: str, 
                         intent_classification: str, response_generated: str):
        """Log healthcare voice command"""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO healthcare_voice_commands 
                (patient_id, command_text, intent_classification, response_generated)
                VALUES (?, ?, ?, ?)
            ''', (patient_id, command_text, intent_classification, response_generated))
            
            conn.commit()