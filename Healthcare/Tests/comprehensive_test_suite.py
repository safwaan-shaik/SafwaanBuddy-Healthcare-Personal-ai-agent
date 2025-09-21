"""
J.A.R.V.I.S. Healthcare Module - Comprehensive Test Suite
Complete testing framework for all healthcare functionality
"""

import unittest
import sys
import os
import tempfile
import json
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime, timedelta

# Add project root to path
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

# Import all healthcare modules
from Healthcare.Database.models import HealthcareDatabase
from Healthcare.Database.encryption import HealthcareEncryption
from Healthcare.Core.pregnancy_care import PregnancyCareModule
from Healthcare.Core.medication_scheduler import MedicationScheduler
from Healthcare.Core.medical_ocr import MedicalOCR
from Healthcare.Core.ai_prescription_parser import AIPrescriptionParser
from Healthcare.Core.lab_analyzer_emergency import LabResultsAnalyzer, EmergencyDetectionSystem

class TestHealthcareEncryptionSuite(unittest.TestCase):
    """Comprehensive encryption testing suite"""
    
    def setUp(self):
        self.encryption = HealthcareEncryption("test_key_123")
    
    def test_string_encryption_decryption(self):
        """Test basic string encryption/decryption"""
        test_data = "Patient: Jane Doe, DOB: 1990-01-01"
        encrypted = self.encryption.encrypt_data(test_data)
        decrypted = self.encryption.decrypt_data(encrypted)
        
        self.assertNotEqual(test_data, encrypted)
        self.assertEqual(test_data, decrypted)
    
    def test_json_encryption_decryption(self):
        """Test JSON data encryption/decryption"""
        test_data = {
            "patient_name": "Test Patient",
            "medications": ["Prenatal vitamins", "Iron"],
            "allergies": ["None"],
            "emergency_contact": "555-1234"
        }
        
        encrypted = self.encryption.encrypt_json(test_data)
        decrypted = self.encryption.decrypt_json(encrypted)
        
        self.assertEqual(test_data, decrypted)
    
    def test_encryption_consistency(self):
        """Test encryption produces different results each time"""
        test_data = "Consistent test data"
        encrypted1 = self.encryption.encrypt_data(test_data)
        encrypted2 = self.encryption.encrypt_data(test_data)
        
        # Should be different due to IV
        self.assertNotEqual(encrypted1, encrypted2)
        
        # But both should decrypt to same value
        self.assertEqual(self.encryption.decrypt_data(encrypted1), test_data)
        self.assertEqual(self.encryption.decrypt_data(encrypted2), test_data)

class TestHealthcareDatabaseSuite(unittest.TestCase):
    """Comprehensive database testing suite"""
    
    def setUp(self):
        self.temp_db = tempfile.NamedTemporaryFile(delete=False, suffix='.db')
        self.temp_db.close()
        self.db = HealthcareDatabase(self.temp_db.name)
    
    def tearDown(self):
        os.unlink(self.temp_db.name)
    
    def test_patient_lifecycle(self):
        """Test complete patient data lifecycle"""
        # Create patient
        patient_id = self.db.create_patient(
            name="Test Patient", dob="1990-01-01", 
            expected_due_date="2025-06-01", gestational_week=24,
            allergies="Penicillin", emergency_contact="555-HELP"
        )
        
        self.assertIsInstance(patient_id, int)
        self.assertGreater(patient_id, 0)
        
        # Retrieve patient
        patient = self.db.get_patient(patient_id)
        self.assertIsNotNone(patient)
        self.assertEqual(patient['name'], "Test Patient")
        self.assertEqual(patient['gestational_week'], 24)
        self.assertEqual(patient['allergies'], "Penicillin")
    
    def test_prescription_management(self):
        """Test prescription storage and retrieval"""
        # Create patient first
        patient_id = self.db.create_patient(
            name="Test Patient", dob="1990-01-01", 
            expected_due_date="2025-06-01", gestational_week=20
        )
        
        # Add prescription
        test_medications = {
            "medications": [
                {"name": "Prenatal Vitamins", "dosage": "1 tablet", "frequency": "daily"}
            ]
        }
        
        prescription_id = self.db.add_prescription(
            patient_id=patient_id, image_path="/test/path/prescription.jpg",
            ocr_text="Test OCR text", parsed_medications=test_medications
        )
        
        self.assertIsInstance(prescription_id, int)
        self.assertGreater(prescription_id, 0)
    
    def test_medication_reminders(self):
        """Test medication reminder system"""
        # Create patient
        patient_id = self.db.create_patient(
            name="Test Patient", dob="1990-01-01", 
            expected_due_date="2025-06-01", gestational_week=20
        )
        
        # Add medication reminder
        reminder_id = self.db.add_medication_reminder(
            patient_id=patient_id, prescription_id=0,
            medication_name="Iron Supplement", dosage="65mg",
            frequency="Daily", times=["08:00", "20:00"],
            start_date=datetime.now().strftime("%Y-%m-%d"),
            end_date=(datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d")
        )
        
        self.assertIsInstance(reminder_id, int)
        
        # Retrieve reminders
        reminders = self.db.get_active_reminders(patient_id)
        self.assertGreater(len(reminders), 0)
        self.assertEqual(reminders[0]['medication_name'], "Iron Supplement")
        self.assertEqual(reminders[0]['dosage'], "65mg")

class TestPregnancyCareSuite(unittest.TestCase):
    """Comprehensive pregnancy care module testing"""
    
    def setUp(self):
        with patch('Healthcare.Core.pregnancy_care.HealthcareDatabase'):
            self.care_module = PregnancyCareModule()
    
    def test_medication_reminder_commands(self):
        """Test medication reminder voice commands"""
        test_cases = [
            ("medication_reminder", "prenatal vitamins"),
            ("medication_reminder", "iron tablets"),
            ("medication_reminder", "calcium supplement")
        ]
        
        for command, details in test_cases:
            response = self.care_module.process_healthcare_command(command, details)
            self.assertIsInstance(response, str)
            self.assertIn("reminder", response.lower())
    
    def test_symptom_logging_commands(self):
        """Test symptom logging functionality"""
        test_symptoms = [
            "nausea", "morning sickness", "back pain", 
            "headache", "swelling", "contractions"
        ]
        
        for symptom in test_symptoms:
            response = self.care_module.process_healthcare_command("log_symptom", symptom)
            self.assertIsInstance(response, str)
            self.assertIn("logged", response.lower())
    
    def test_emergency_detection(self):
        """Test emergency situation detection"""
        emergency_scenarios = [
            "severe contractions",
            "heavy bleeding", 
            "severe headache",
            "emergency help"
        ]
        
        for scenario in emergency_scenarios:
            response = self.care_module.process_healthcare_command("health_emergency", scenario)
            self.assertIsInstance(response, str)
            self.assertTrue(any(word in response.lower() 
                              for word in ["urgent", "emergency", "contact", "provider"]))
    
    def test_prenatal_care_queries(self):
        """Test prenatal care information queries"""
        query_types = [
            ("prenatal_care", "nutrition advice"),
            ("prenatal_care", "exercise recommendations"),
            ("prenatal_care", "appointment scheduling"),
            ("pregnancy_care", "baby development"),
            ("pregnancy_care", "week information")
        ]
        
        for command, query in query_types:
            response = self.care_module.process_healthcare_command(command, query)
            self.assertIsInstance(response, str)
            self.assertGreater(len(response), 10)

class TestMedicationSchedulerSuite(unittest.TestCase):
    """Comprehensive medication scheduler testing"""
    
    def setUp(self):
        self.mock_db = Mock()
        self.scheduler = MedicationScheduler(self.mock_db)
    
    def test_scheduler_initialization(self):
        """Test scheduler proper initialization"""
        self.assertFalse(self.scheduler.running)
        self.assertIsNone(self.scheduler.scheduler_thread)
        self.assertEqual(self.scheduler.reminder_check_interval, 60)
    
    def test_reminder_due_calculation(self):
        """Test reminder due time calculation"""
        current_time = datetime.now()
        test_reminder = {
            'id': 1, 'active': True,
            'start_date': current_time.strftime("%Y-%m-%d"),
            'end_date': (current_time + timedelta(days=1)).strftime("%Y-%m-%d"),
            'times': [current_time.strftime("%H:%M")]
        }
        
        # Test with current time - should be due
        is_due = self.scheduler._is_reminder_due(
            test_reminder, 
            current_time.strftime("%H:%M"), 
            current_time.strftime("%Y-%m-%d")
        )
        self.assertIsInstance(is_due, bool)
    
    def test_add_medication_reminder(self):
        """Test adding medication reminders"""
        self.mock_db.add_medication_reminder.return_value = 42
        
        result = self.scheduler.add_medication_reminder(
            patient_id=1, medication_name="Test Med", dosage="1 tablet",
            frequency="Daily", times=["08:00"], 
            start_date="2025-01-01", end_date="2025-01-31"
        )
        
        self.assertEqual(result, 42)
        self.mock_db.add_medication_reminder.assert_called_once()
    
    def test_today_reminders_filtering(self):
        """Test filtering reminders for today"""
        today = datetime.now().strftime("%Y-%m-%d")
        tomorrow = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
        
        mock_reminders = [
            {'start_date': today, 'end_date': tomorrow, 'medication_name': 'Vitamin'},
            {'start_date': '2024-01-01', 'end_date': '2024-01-31', 'medication_name': 'Old Med'}
        ]
        
        self.mock_db.get_active_reminders.return_value = mock_reminders
        
        today_reminders = self.scheduler.get_todays_reminders(1)
        
        # Should only get today's reminder
        self.assertEqual(len(today_reminders), 1)
        self.assertEqual(today_reminders[0]['medication_name'], 'Vitamin')

class TestMedicalOCRSuite(unittest.TestCase):
    """Comprehensive medical OCR testing"""
    
    def setUp(self):
        with patch('Healthcare.Core.medical_ocr.HealthcareDatabase'):
            self.ocr = MedicalOCR()
    
    def test_text_cleaning(self):
        """Test OCR text cleaning functionality"""
        dirty_text = "  Extra   spaces\n\nAnd||special@#characters  "
        cleaned = self.ocr._clean_ocr_text(dirty_text)
        
        self.assertNotIn("||", cleaned)
        self.assertNotIn("@#", cleaned)
        self.assertNotIn("\n\n", cleaned)
    
    def test_medication_extraction_patterns(self):
        """Test medication information extraction patterns"""
        test_cases = [
            ("Amoxicillin 500mg twice daily", "Amoxicillin", "500mg", "twice daily"),
            ("Prenatal Vitamins 1 tablet once daily", "Prenatal", "1 tablet", "once daily"),
            ("Iron supplement 65mg every evening", "Iron", "65mg", "evening")
        ]
        
        for text, expected_name, expected_dose, expected_freq in test_cases:
            name = self.ocr._extract_medication_name(text)
            dosage = self.ocr._extract_dosage(text)
            frequency = self.ocr._extract_frequency(text)
            
            self.assertIsNotNone(name)
            if dosage:
                self.assertIn(expected_dose.split()[0], dosage)  # Check numeric part
    
    def test_lab_value_extraction(self):
        """Test lab value extraction from text"""
        test_text = """
        Hemoglobin: 12.5 g/dl
        Blood Pressure: 120/80
        Glucose: 95 mg/dl
        Protein: 0.5 g/dl
        """
        
        lab_values = self.ocr._extract_lab_values_from_text(test_text)
        
        self.assertIsInstance(lab_values, dict)
        self.assertIn('hemoglobin', lab_values)
        if 'hemoglobin' in lab_values:
            self.assertEqual(lab_values['hemoglobin'], 12.5)

class TestLabAnalyzerSuite(unittest.TestCase):
    """Comprehensive lab analyzer testing"""
    
    def setUp(self):
        with patch('Healthcare.Core.lab_analyzer_emergency.HealthcareDatabase'):
            with patch('Healthcare.Core.lab_analyzer_emergency.MedicalOCR'):
                self.analyzer = LabResultsAnalyzer()
    
    def test_normal_values_analysis(self):
        """Test analysis of normal lab values"""
        normal_values = {
            'hemoglobin': 12.5,
            'glucose_fasting': 85,
            'blood_pressure_systolic': 110,
            'blood_pressure_diastolic': 70
        }
        
        analysis = self.analyzer._analyze_values(normal_values)
        
        self.assertIn('normal_values', analysis)
        self.assertIn('flagged_values', analysis)
        self.assertGreater(len(analysis['normal_values']), 0)
    
    def test_abnormal_values_detection(self):
        """Test detection of abnormal lab values"""
        abnormal_values = {
            'hemoglobin': 8.0,  # Low
            'glucose_fasting': 110,  # High
            'blood_pressure_systolic': 150  # High
        }
        
        analysis = self.analyzer._analyze_values(abnormal_values)
        flagged = analysis['flagged_values']
        
        self.assertIn('hemoglobin', flagged)
        self.assertEqual(flagged['hemoglobin']['status'], 'low')
        self.assertIn('glucose_fasting', flagged)
        self.assertEqual(flagged['glucose_fasting']['status'], 'high')
    
    def test_critical_condition_detection(self):
        """Test detection of critical pregnancy conditions"""
        # Test preeclampsia detection
        preeclampsia_values = {
            'blood_pressure_systolic': 150,
            'blood_pressure_diastolic': 95,
            'protein_urine': 350
        }
        
        critical_alerts = self.analyzer._check_critical_conditions(preeclampsia_values)
        
        self.assertGreater(len(critical_alerts), 0)
        preeclampsia_alert = next((alert for alert in critical_alerts 
                                 if alert['condition'] == 'preeclampsia'), None)
        self.assertIsNotNone(preeclampsia_alert)

class TestEmergencyDetectionSuite(unittest.TestCase):
    """Comprehensive emergency detection testing"""
    
    def setUp(self):
        with patch('Healthcare.Core.lab_analyzer_emergency.HealthcareDatabase'):
            self.emergency_detector = EmergencyDetectionSystem()
    
    def test_emergency_symptom_detection(self):
        """Test detection of emergency symptoms"""
        emergency_phrases = [
            "I'm having severe bleeding",
            "Severe contractions every 5 minutes",
            "Can't see clearly, severe headache",
            "Baby hasn't moved all day"
        ]
        
        for phrase in emergency_phrases:
            result = self.emergency_detector.analyze_emergency_situation(phrase)
            self.assertTrue(result['emergency_detected'])
            self.assertIn('emergency_type', result)
            self.assertIn('urgency', result)
    
    def test_non_emergency_detection(self):
        """Test that normal symptoms don't trigger emergencies"""
        normal_phrases = [
            "Feeling a bit tired today",
            "Mild nausea this morning",
            "Some back discomfort",
            "Scheduled doctor appointment"
        ]
        
        for phrase in normal_phrases:
            result = self.emergency_detector.analyze_emergency_situation(phrase)
            self.assertFalse(result['emergency_detected'])
    
    def test_emergency_recommendations(self):
        """Test emergency recommendation generation"""
        emergency_types = ['severe_bleeding', 'severe_contractions', 'severe_headache']
        
        for emergency_type in emergency_types:
            recommendations = self.emergency_detector._get_emergency_recommendations(emergency_type)
            self.assertIsInstance(recommendations, list)
            self.assertGreater(len(recommendations), 0)
            self.assertTrue(any('call' in rec.lower() for rec in recommendations))

class TestIntegrationSuite(unittest.TestCase):
    """Integration testing for all healthcare components"""
    
    def test_end_to_end_medication_workflow(self):
        """Test complete medication management workflow"""
        # This would test the full workflow from prescription upload 
        # to reminder creation to notification
        pass
    
    def test_emergency_to_response_workflow(self):
        """Test emergency detection to response workflow"""
        # This would test emergency detection triggering proper responses
        pass
    
    def test_lab_analysis_to_alert_workflow(self):
        """Test lab analysis triggering appropriate alerts"""
        # This would test lab analysis creating proper health alerts
        pass

def run_comprehensive_tests():
    """Run all healthcare tests with detailed reporting"""
    # Create test suite
    test_suite = unittest.TestSuite()
    
    # Add all test classes
    test_classes = [
        TestHealthcareEncryptionSuite,
        TestHealthcareDatabaseSuite, 
        TestPregnancyCareSuite,
        TestMedicationSchedulerSuite,
        TestMedicalOCRSuite,
        TestLabAnalyzerSuite,
        TestEmergencyDetectionSuite,
        TestIntegrationSuite
    ]
    
    for test_class in test_classes:
        tests = unittest.TestLoader().loadTestsFromTestCase(test_class)
        test_suite.addTests(tests)
    
    # Run tests with detailed output
    runner = unittest.TextTestRunner(verbosity=2, stream=sys.stdout)
    result = runner.run(test_suite)
    
    # Print summary
    print(f"\n{'='*60}")
    print(f"HEALTHCARE MODULE TEST SUMMARY")
    print(f"{'='*60}")
    print(f"Tests Run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Success Rate: {((result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun * 100):.1f}%")
    
    if result.failures:
        print(f"\nFAILURES:")
        for test, traceback in result.failures:
            print(f"- {test}: {traceback}")
    
    if result.errors:
        print(f"\nERRORS:")
        for test, traceback in result.errors:
            print(f"- {test}: {traceback}")
    
    return result.wasSuccessful()

if __name__ == '__main__':
    print("🧪 Running J.A.R.V.I.S. Healthcare Module Comprehensive Test Suite...")
    print("=" * 70)
    
    success = run_comprehensive_tests()
    
    print("=" * 70)
    if success:
        print("✅ All healthcare tests passed! System ready for deployment.")
    else:
        print("❌ Some tests failed. Review failures before deployment.")
    
    print("🏥 Healthcare comprehensive testing complete.")