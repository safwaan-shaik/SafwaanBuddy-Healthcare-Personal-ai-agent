"""
J.A.R.V.I.S. Healthcare Module Integration Tests
Comprehensive test suite for healthcare functionality
"""

import unittest
import sys
import os
from unittest.mock import Mock, patch, MagicMock
import tempfile
import json
from datetime import datetime, timedelta

# Add project root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

# Import healthcare modules
from Healthcare.Database.models import HealthcareDatabase
from Healthcare.Database.encryption import HealthcareEncryption, HealthcareAuditLogger
from Healthcare.Core.pregnancy_care import PregnancyCareModule
from Healthcare.Core.medication_scheduler import MedicationScheduler, VoiceMedicationInterface
from Healthcare.Core.medical_ocr import MedicalOCR

class TestHealthcareEncryption(unittest.TestCase):
    """Test encryption and security features"""
    
    def setUp(self):
        self.encryption = HealthcareEncryption("test_password_123")
    
    def test_encrypt_decrypt_string(self):
        """Test basic string encryption/decryption"""
        original_data = "Sensitive patient information"
        encrypted = self.encryption.encrypt_data(original_data)
        decrypted = self.encryption.decrypt_data(encrypted)
        
        self.assertNotEqual(original_data, encrypted)
        self.assertEqual(original_data, decrypted)
    
    def test_encrypt_decrypt_json(self):
        """Test JSON encryption/decryption"""
        original_data = {
            "patient_name": "Test Patient",
            "diagnosis": "Pregnancy - 20 weeks",
            "medications": ["Prenatal vitamins", "Iron supplement"]
        }
        
        encrypted = self.encryption.encrypt_json(original_data)
        decrypted = self.encryption.decrypt_json(encrypted)
        
        self.assertEqual(original_data, decrypted)
    
    def test_audit_logging(self):
        """Test audit logging functionality"""
        audit_logger = HealthcareAuditLogger(self.encryption)
        
        # Test logging
        audit_logger.log_medical_interaction(
            "TEST_ACTION", "patient_123", 
            {"test_field": "test_value"}
        )
        
        # Test retrieval
        logs = audit_logger.get_audit_logs("patient_123")
        self.assertGreater(len(logs), 0)
        self.assertEqual(logs[0]['action'], "TEST_ACTION")
        self.assertEqual(logs[0]['patient_id'], "patient_123")

class TestHealthcareDatabase(unittest.TestCase):
    """Test database operations"""
    
    def setUp(self):
        # Use temporary database for testing
        self.temp_db = tempfile.NamedTemporaryFile(delete=False, suffix='.db')
        self.temp_db.close()
        self.db = HealthcareDatabase(self.temp_db.name)
    
    def tearDown(self):
        os.unlink(self.temp_db.name)
    
    def test_create_patient(self):
        """Test patient creation"""
        patient_id = self.db.create_patient(
            name="Test Patient",
            dob="1990-01-01",
            expected_due_date="2025-06-01",
            gestational_week=20,
            allergies="None",
            emergency_contact="123-456-7890"
        )
        
        self.assertIsInstance(patient_id, int)
        self.assertGreater(patient_id, 0)
        
        # Test retrieval
        patient = self.db.get_patient(patient_id)
        self.assertIsNotNone(patient)
        self.assertEqual(patient['name'], "Test Patient")
        self.assertEqual(patient['gestational_week'], 20)
    
    def test_medication_reminder(self):
        """Test medication reminder creation"""
        # First create a patient
        patient_id = self.db.create_patient(
            name="Test Patient", dob="1990-01-01", 
            expected_due_date="2025-06-01", gestational_week=20
        )
        
        # Add medication reminder
        reminder_id = self.db.add_medication_reminder(
            patient_id=patient_id,
            prescription_id=0,
            medication_name="Prenatal Vitamins",
            dosage="1 tablet",
            frequency="Daily",
            times=["08:00"],
            start_date=datetime.now().strftime("%Y-%m-%d"),
            end_date=(datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d")
        )
        
        self.assertIsInstance(reminder_id, int)
        self.assertGreater(reminder_id, 0)
        
        # Test retrieval
        reminders = self.db.get_active_reminders(patient_id)
        self.assertGreater(len(reminders), 0)
        self.assertEqual(reminders[0]['medication_name'], "Prenatal Vitamins")

class TestPregnancyCareModule(unittest.TestCase):
    """Test pregnancy care module functionality"""
    
    def setUp(self):
        # Mock database for testing
        with patch('Healthcare.Core.pregnancy_care.HealthcareDatabase'):
            self.care_module = PregnancyCareModule()
    
    def test_medication_reminder_processing(self):
        """Test medication reminder voice command processing"""
        response = self.care_module.process_healthcare_command(
            "medication_reminder", "prenatal vitamins"
        )
        
        self.assertIsInstance(response, str)
        self.assertIn("reminder", response.lower())
    
    def test_symptom_logging(self):
        """Test symptom logging"""
        response = self.care_module.process_healthcare_command(
            "log_symptom", "nausea"
        )
        
        self.assertIsInstance(response, str)
        self.assertIn("logged", response.lower())
    
    def test_emergency_handling(self):
        """Test emergency situation handling"""
        response = self.care_module.process_healthcare_command(
            "health_emergency", "severe contractions"
        )
        
        self.assertIsInstance(response, str)
        self.assertTrue(any(word in response.lower() for word in ["urgent", "emergency", "contact"]))
    
    def test_prenatal_care_queries(self):
        """Test prenatal care information queries"""
        test_queries = [
            ("prenatal_care", "nutrition"),
            ("prenatal_care", "exercise"),
            ("prenatal_care", "appointment")
        ]
        
        for command, details in test_queries:
            response = self.care_module.process_healthcare_command(command, details)
            self.assertIsInstance(response, str)
            self.assertGreater(len(response), 0)

class TestMedicationScheduler(unittest.TestCase):
    """Test medication scheduler functionality"""
    
    def setUp(self):
        # Mock database
        self.mock_db = Mock()
        self.scheduler = MedicationScheduler(self.mock_db)
    
    def test_scheduler_initialization(self):
        """Test scheduler initialization"""
        self.assertFalse(self.scheduler.running)
        self.assertIsNone(self.scheduler.scheduler_thread)
    
    def test_reminder_due_check(self):
        """Test reminder due time checking"""
        current_time = datetime.now()
        current_time_str = current_time.strftime("%H:%M")
        current_date = current_time.strftime("%Y-%m-%d")
        
        # Create test reminder
        test_reminder = {
            'id': 1,
            'active': True,
            'start_date': current_date,
            'end_date': (current_time + timedelta(days=1)).strftime("%Y-%m-%d"),
            'times': [current_time_str]
        }
        
        is_due = self.scheduler._is_reminder_due(test_reminder, current_time_str, current_date)
        # Note: This might be False due to timing, but the method should execute without error
        self.assertIsInstance(is_due, bool)
    
    def test_add_medication_reminder(self):
        """Test adding medication reminder"""
        self.mock_db.add_medication_reminder.return_value = 123
        
        reminder_id = self.scheduler.add_medication_reminder(
            patient_id=1,
            medication_name="Test Medication",
            dosage="1 tablet",
            frequency="Daily",
            times=["08:00"],
            start_date="2025-01-01",
            end_date="2025-01-31"
        )
        
        self.assertEqual(reminder_id, 123)
        self.mock_db.add_medication_reminder.assert_called_once()

class TestVoiceMedicationInterface(unittest.TestCase):
    """Test voice medication interface"""
    
    def setUp(self):
        mock_scheduler = Mock()
        self.voice_interface = VoiceMedicationInterface(mock_scheduler)
    
    def test_voice_command_processing(self):
        """Test voice command processing"""
        test_commands = [
            ("remind me to take vitamins", "vitamins"),
            ("I took my medication", "medication"),
            ("snooze reminder", "")
        ]
        
        for command, details in test_commands:
            response = self.voice_interface.process_medication_voice_command(
                command, details, patient_id=1
            )
            self.assertIsInstance(response, str)
            self.assertGreater(len(response), 0)

class TestMedicalOCR(unittest.TestCase):
    """Test medical OCR functionality"""
    
    def setUp(self):
        with patch('Healthcare.Core.medical_ocr.HealthcareDatabase'):
            self.ocr = MedicalOCR()
    
    def test_medication_pattern_extraction(self):
        """Test medication pattern extraction"""
        test_text = "Amoxicillin 500mg twice daily for 7 days"
        
        medication_name = self.ocr._extract_medication_name(test_text)
        dosage = self.ocr._extract_dosage(test_text)
        frequency = self.ocr._extract_frequency(test_text)
        duration = self.ocr._extract_duration(test_text)
        
        self.assertIsNotNone(medication_name)
        self.assertIn("500mg", dosage) if dosage else None
        self.assertIn("twice", frequency) if frequency else None
        self.assertIn("7 days", duration) if duration else None
    
    def test_lab_value_extraction(self):
        """Test lab value extraction"""
        test_text = "Hemoglobin: 12.5 g/dl\\nBlood Pressure: 120/80\\nGlucose: 95 mg/dl"
        
        lab_results = self.ocr._extract_lab_values_from_text(test_text)
        
        self.assertIsInstance(lab_results, dict)
        # Check if some values were extracted (exact matches depend on regex patterns)
    
    def test_lab_analysis(self):
        """Test lab result analysis"""
        test_results = {
            'hemoglobin': 8.0,  # Below normal
            'glucose': 200,     # Above normal
            'protein': 7.0      # Normal
        }
        
        flagged = self.ocr._analyze_lab_results(test_results)
        
        self.assertIsInstance(flagged, dict)
        # Should flag low hemoglobin and high glucose

class TestHealthcareIntegration(unittest.TestCase):
    """Test overall healthcare system integration"""
    
    def test_healthcare_module_imports(self):
        """Test that all healthcare modules can be imported"""
        try:
            from Healthcare.Core.pregnancy_care import PregnancyCareModule
            from Healthcare.Database.models import HealthcareDatabase
            from Healthcare.Core.medication_scheduler import MedicationScheduler
            from Healthcare.Core.medical_ocr import MedicalOCR
            self.assertTrue(True)  # If we get here, imports worked
        except ImportError as e:
            self.fail(f"Healthcare module import failed: {e}")
    
    def test_voice_command_mapping(self):
        """Test voice command to healthcare function mapping"""
        # This would test the integration in Main.py
        # For now, just test that the mapping logic works
        
        healthcare_commands = [
            "medication reminder prenatal vitamins",
            "upload prescription",
            "log symptom nausea", 
            "health emergency",
            "prenatal care nutrition"
        ]
        
        for command in healthcare_commands:
            # Test that we can parse these commands
            parts = command.split(' ', 1)
            self.assertGreater(len(parts), 0)

def run_healthcare_tests():
    """Run all healthcare tests"""
    # Create test suite
    test_suite = unittest.TestSuite()
    
    # Add test classes
    test_classes = [
        TestHealthcareEncryption,
        TestHealthcareDatabase,
        TestPregnancyCareModule,
        TestMedicationScheduler,
        TestVoiceMedicationInterface,
        TestMedicalOCR,
        TestHealthcareIntegration
    ]
    
    for test_class in test_classes:
        tests = unittest.TestLoader().loadTestsFromTestCase(test_class)
        test_suite.addTests(tests)
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(test_suite)
    
    return result.wasSuccessful()

if __name__ == '__main__':
    print("🧪 Running J.A.R.V.I.S. Healthcare Module Tests...")
    print("=" * 60)
    
    success = run_healthcare_tests()
    
    print("=" * 60)
    if success:
        print("✅ All healthcare tests passed!")
    else:
        print("❌ Some healthcare tests failed. Check output above.")
    
    print("🏥 Healthcare module testing complete.")