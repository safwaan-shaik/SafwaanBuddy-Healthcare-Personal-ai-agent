"""
J.A.R.V.I.S. Lab Results Analyzer & Emergency Detection System
Advanced health monitoring with intelligent analysis and emergency protocols
"""

import os
import sys
import json
import re
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import threading

# Import existing healthcare components
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from Healthcare.Database.models import HealthcareDatabase
from Healthcare.Core.medical_ocr import MedicalOCR
from Backend.TextToSpeech import TextToSpeech
from Frontend.GUI import ShowTextToScreen, SetAssistantStatus

class LabResultsAnalyzer:
    """Advanced lab results analyzer with pregnancy-specific analysis"""
    
    def __init__(self):
        self.healthcare_db = HealthcareDatabase()
        self.medical_ocr = MedicalOCR()
        
        # Pregnancy normal ranges
        self.normal_ranges = {
            'hemoglobin': {'min': 11.0, 'max': 14.0, 'unit': 'g/dL', 'critical_low': 9.0},
            'glucose_fasting': {'min': 70, 'max': 95, 'unit': 'mg/dL', 'critical_high': 125},
            'protein_urine': {'min': 0, 'max': 150, 'unit': 'mg/24hr', 'critical_high': 300},
            'blood_pressure_systolic': {'min': 90, 'max': 120, 'unit': 'mmHg', 'critical_high': 140},
            'blood_pressure_diastolic': {'min': 60, 'max': 80, 'unit': 'mmHg', 'critical_high': 90},
        }
        
        # Critical conditions
        self.critical_conditions = {
            'preeclampsia': {
                'indicators': ['bp_systolic > 140', 'bp_diastolic > 90', 'protein > 300'],
                'message': 'Possible preeclampsia detected. Seek immediate medical attention.'
            },
            'severe_anemia': {
                'indicators': ['hemoglobin < 9.0'],
                'message': 'Severe anemia detected. Contact healthcare provider immediately.'
            },
            'gestational_diabetes': {
                'indicators': ['glucose_fasting > 92'],
                'message': 'Possible gestational diabetes. Schedule appointment with healthcare provider.'
            }
        }
        
        print("✅ Lab Results Analyzer initialized")
    
    def analyze_lab_results(self, image_path: str, gestational_week: int = 20) -> Dict[str, Any]:
        """Comprehensive lab results analysis"""
        try:
            # Extract lab data using OCR
            ocr_result = self.medical_ocr.parse_lab_results(image_path)
            
            if not ocr_result.get('success'):
                return {'success': False, 'error': ocr_result.get('error', 'Failed to process')}
            
            lab_values = ocr_result.get('results', {})
            
            # Perform analysis
            analysis = self._analyze_values(lab_values)
            critical_alerts = self._check_critical_conditions(lab_values)
            
            # Store results
            result_id = self.healthcare_db.add_lab_result(
                patient_id=1, test_date=datetime.now().strftime('%Y-%m-%d'),
                test_type='Lab Analysis', results=lab_values,
                flagged_values=analysis.get('flagged_values', {}),
                urgency_level='critical' if critical_alerts else 'normal'
            )
            
            return {
                'success': True, 'result_id': result_id, 'gestational_week': gestational_week,
                'lab_values': lab_values, 'analysis': analysis, 'critical_alerts': critical_alerts,
                'recommendations': self._generate_recommendations(analysis, critical_alerts),
                'requires_immediate_attention': len(critical_alerts) > 0
            }
            
        except Exception as e:
            return {'success': False, 'error': f'Analysis failed: {str(e)}'}
    
    def _analyze_values(self, lab_values: Dict[str, Any]) -> Dict[str, Any]:
        """Analyze lab values against normal ranges"""
        analysis = {'normal_values': {}, 'flagged_values': {}}
        
        for test_name, value in lab_values.items():
            if test_name in self.normal_ranges and isinstance(value, (int, float)):
                range_info = self.normal_ranges[test_name]
                
                test_analysis = {
                    'value': value, 'unit': range_info['unit'],
                    'normal_range': f"{range_info['min']}-{range_info['max']} {range_info['unit']}",
                    'status': 'normal'
                }
                
                if value < range_info['min']:
                    test_analysis['status'] = 'low'
                    test_analysis['severity'] = 'severe' if value < range_info.get('critical_low', 0) else 'mild'
                    analysis['flagged_values'][test_name] = test_analysis
                elif value > range_info['max']:
                    test_analysis['status'] = 'high'
                    test_analysis['severity'] = 'severe' if value > range_info.get('critical_high', 999) else 'mild'
                    analysis['flagged_values'][test_name] = test_analysis
                else:
                    analysis['normal_values'][test_name] = test_analysis
        
        return analysis
    
    def _check_critical_conditions(self, lab_values: Dict[str, Any]) -> List[Dict[str, Any]]:
        """Check for critical pregnancy conditions"""
        critical_alerts = []
        
        # Check preeclampsia
        if (lab_values.get('blood_pressure_systolic', 0) > 140 and 
            lab_values.get('blood_pressure_diastolic', 0) > 90 and 
            lab_values.get('protein_urine', 0) > 300):
            critical_alerts.append({
                'condition': 'preeclampsia',
                'message': self.critical_conditions['preeclampsia']['message'],
                'urgency': 'immediate'
            })
        
        # Check severe anemia
        if lab_values.get('hemoglobin', 15) < 9.0:
            critical_alerts.append({
                'condition': 'severe_anemia',
                'message': self.critical_conditions['severe_anemia']['message'],
                'urgency': 'immediate'
            })
        
        # Check gestational diabetes
        if lab_values.get('glucose_fasting', 70) > 92:
            critical_alerts.append({
                'condition': 'gestational_diabetes',
                'message': self.critical_conditions['gestational_diabetes']['message'],
                'urgency': 'urgent'
            })
        
        return critical_alerts
    
    def _generate_recommendations(self, analysis: Dict[str, Any], critical_alerts: List[Dict[str, Any]]) -> List[str]:
        """Generate personalized recommendations"""
        recommendations = []
        
        if critical_alerts:
            recommendations.extend([
                "Seek immediate medical attention for critical findings.",
                "Contact your healthcare provider or go to emergency room."
            ])
        
        flagged_values = analysis.get('flagged_values', {})
        
        if 'hemoglobin' in flagged_values and flagged_values['hemoglobin']['status'] == 'low':
            recommendations.append("Increase iron-rich foods and consider iron supplementation.")
        
        if 'glucose_fasting' in flagged_values and flagged_values['glucose_fasting']['status'] == 'high':
            recommendations.append("Monitor carbohydrate intake and discuss with healthcare provider.")
        
        if not recommendations:
            recommendations.extend([
                "Continue current prenatal care routine.",
                "Maintain healthy diet and regular prenatal vitamins."
            ])
        
        return recommendations

class EmergencyDetectionSystem:
    """Advanced emergency detection and response system"""
    
    def __init__(self):
        self.healthcare_db = HealthcareDatabase()
        self.lab_analyzer = LabResultsAnalyzer()
        
        # Emergency symptoms patterns
        self.emergency_symptoms = {
            'severe_bleeding': {
                'keywords': ['heavy bleeding', 'severe bleeding', 'hemorrhage'],
                'urgency': 'immediate', 'action': 'call_emergency_services'
            },
            'severe_contractions': {
                'keywords': ['severe contractions', 'labor pain', 'water broke'],
                'urgency': 'immediate', 'action': 'call_healthcare_provider'
            },
            'severe_headache': {
                'keywords': ['severe headache', 'blurred vision', 'seeing spots'],
                'urgency': 'urgent', 'action': 'call_healthcare_provider'
            },
            'decreased_movement': {
                'keywords': ['baby not moving', 'decreased movement', 'no kicks'],
                'urgency': 'urgent', 'action': 'call_healthcare_provider'
            }
        }
        
        print("✅ Emergency Detection System initialized")
    
    def analyze_emergency_situation(self, user_input: str) -> Dict[str, Any]:
        """Analyze user input for emergency situations"""
        try:
            user_input_lower = user_input.lower()
            detected_emergencies = []
            
            # Check for emergency symptoms
            for emergency_type, emergency_info in self.emergency_symptoms.items():
                for keyword in emergency_info['keywords']:
                    if keyword in user_input_lower:
                        detected_emergencies.append({
                            'type': emergency_type,
                            'urgency': emergency_info['urgency'],
                            'action': emergency_info['action']
                        })
                        break
            
            if detected_emergencies:
                # Select highest priority emergency
                highest_priority = max(detected_emergencies, 
                                     key=lambda x: 1 if x['urgency'] == 'immediate' else 0)
                
                return self._handle_emergency(highest_priority, user_input)
            else:
                return {
                    'emergency_detected': False,
                    'message': 'No emergency detected. Continue monitoring symptoms.',
                    'recommendations': ['Contact healthcare provider if symptoms worsen']
                }
                
        except Exception as e:
            return {
                'emergency_detected': True, 'error': str(e),
                'message': 'If this is an emergency, call 911 immediately.',
                'recommendations': ['Call 911 if immediate danger']
            }
    
    def _handle_emergency(self, emergency: Dict[str, Any], user_input: str) -> Dict[str, Any]:
        """Handle detected emergency situation"""
        try:
            emergency_type = emergency['type']
            urgency = emergency['urgency']
            
            # Log emergency
            self.healthcare_db.log_voice_command(
                patient_id=1, command_text=user_input,
                intent_classification='EMERGENCY',
                response_generated=f"Emergency detected: {emergency_type}"
            )
            
            # Generate response
            if urgency == 'immediate':
                message = f"MEDICAL EMERGENCY: {emergency_type}. Call 911 immediately!"
                TextToSpeech(message)
                ShowTextToScreen(f"🚨 EMERGENCY: {message}")
            else:
                message = f"Urgent situation: {emergency_type}. Contact healthcare provider."
                TextToSpeech(message)
                ShowTextToScreen(f"⚠️ URGENT: {message}")
            
            return {
                'emergency_detected': True, 'emergency_type': emergency_type,
                'urgency': urgency, 'message': message,
                'recommendations': self._get_emergency_recommendations(emergency_type),
                'timestamp': datetime.now().isoformat()
            }
            
        except Exception as e:
            return {
                'emergency_detected': True, 'error': str(e),
                'message': 'Emergency protocol error. Seek immediate medical attention.'
            }
    
    def _get_emergency_recommendations(self, emergency_type: str) -> List[str]:
        """Get specific recommendations for emergency type"""
        base_recommendations = ["Call healthcare provider or 911", "Do not delay seeking help"]
        
        specific_recommendations = {
            'severe_bleeding': ["Lie down and elevate legs", "Apply gentle pressure if external"],
            'severe_contractions': ["Time contractions", "Gather hospital bag"],
            'severe_headache': ["Rest in dark room", "Monitor vision changes"],
            'decreased_movement': ["Try cold drink", "Lie on left side", "Count movements"]
        }
        
        return base_recommendations + specific_recommendations.get(emergency_type, [])

# Global instances
lab_analyzer = LabResultsAnalyzer()
emergency_detector = EmergencyDetectionSystem()

# Utility functions
def analyze_lab_results_from_image(image_path: str, gestational_week: int = 20) -> Dict[str, Any]:
    """Analyze lab results from image"""
    return lab_analyzer.analyze_lab_results(image_path, gestational_week)

def detect_emergency_from_text(user_input: str) -> Dict[str, Any]:
    """Detect emergency from user input"""
    return emergency_detector.analyze_emergency_situation(user_input)

def get_health_risk_assessment(lab_values: Dict[str, Any]) -> Dict[str, Any]:
    """Get health risk assessment from lab values"""
    analysis = lab_analyzer._analyze_values(lab_values)
    critical_alerts = lab_analyzer._check_critical_conditions(lab_values)
    
    flagged_count = len(analysis.get('flagged_values', {}))
    
    if critical_alerts:
        risk_level = 'high'
        risk_message = 'Critical abnormalities detected requiring immediate attention.'
    elif flagged_count >= 2:
        risk_level = 'moderate'
        risk_message = 'Multiple abnormal values detected. Follow up with healthcare provider.'
    elif flagged_count > 0:
        risk_level = 'low'
        risk_message = 'Some values outside normal range. Monitor closely.'
    else:
        risk_level = 'normal'
        risk_message = 'All values within normal pregnancy ranges.'
    
    return {
        'risk_level': risk_level, 'risk_message': risk_message,
        'flagged_count': flagged_count, 'critical_alerts': critical_alerts,
        'recommendations': lab_analyzer._generate_recommendations(analysis, critical_alerts)
    }