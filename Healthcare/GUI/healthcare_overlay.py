"""
J.A.R.V.I.S. Healthcare GUI Overlay
Healthcare dashboard components integrated with existing PyQt5 interface
"""

import os
import sys
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import json

from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, QFrame, 
                           QTextEdit, QPushButton, QScrollArea, QGridLayout,
                           QProgressBar, QTabWidget, QTableWidget, QTableWidgetItem,
                           QSizePolicy, QSpacerItem, QGroupBox, QListWidget, QListWidgetItem)
from PyQt5.QtGui import QIcon, QPixmap, QFont, QColor, QPalette
from PyQt5.QtCore import Qt, QTimer, QSize, pyqtSignal

# Import existing J.A.R.V.I.S. GUI utilities
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from Frontend.GUI import GraphicsDirectoryPath, TempDirectoryPath
from Healthcare.Database.models import HealthcareDatabase
from Healthcare.Core.pregnancy_care import PregnancyCareModule

class HealthcareStatusWidget(QWidget):
    """
    Widget showing current healthcare status and key metrics
    """
    
    def __init__(self):
        super().__init__()
        self.healthcare_db = HealthcareDatabase()
        self.setup_ui()
        
        # Update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_status)
        self.update_timer.start(30000)  # Update every 30 seconds
    
    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(10, 10, 10, 10)
        
        # Healthcare header
        header = QLabel("🏥 Healthcare Dashboard")
        header.setStyleSheet("""
            QLabel {
                color: #4CAF50;
                font-size: 18px;
                font-weight: bold;
                padding: 10px;
                background-color: rgba(76, 175, 80, 0.1);
                border-radius: 8px;
                border: 2px solid #4CAF50;
            }
        """)
        header.setAlignment(Qt.AlignCenter)
        layout.addWidget(header)
        
        # Current pregnancy status
        self.pregnancy_status = QLabel("📅 Week 20 • Second Trimester")
        self.pregnancy_status.setStyleSheet("""
            QLabel {
                color: #2196F3;
                font-size: 14px;
                padding: 8px;
                background-color: rgba(33, 150, 243, 0.1);
                border-radius: 6px;
                border: 1px solid #2196F3;
            }
        """)
        layout.addWidget(self.pregnancy_status)
        
        # Medication reminders today
        self.medication_status = QLabel("💊 No medication reminders today")
        self.medication_status.setStyleSheet("""
            QLabel {
                color: #FF9800;
                font-size: 12px;
                padding: 6px;
                background-color: rgba(255, 152, 0, 0.1);
                border-radius: 4px;
                border: 1px solid #FF9800;
            }
        """)
        layout.addWidget(self.medication_status)
        
        # Health alerts
        self.health_alerts = QLabel("⚡ All systems normal")
        self.health_alerts.setStyleSheet("""
            QLabel {
                color: #4CAF50;
                font-size: 12px;
                padding: 6px;
                background-color: rgba(76, 175, 80, 0.1);
                border-radius: 4px;
                border: 1px solid #4CAF50;
            }
        """)
        layout.addWidget(self.health_alerts)
        
        # Quick actions
        actions_layout = QHBoxLayout()
        
        self.upload_prescription_btn = QPushButton("📋 Upload Prescription")
        self.upload_prescription_btn.setStyleSheet("""
            QPushButton {
                background-color: #2196F3;
                color: white;
                border: none;
                padding: 8px;
                border-radius: 4px;
                font-size: 10px;
            }
            QPushButton:hover {
                background-color: #1976D2;
            }
        """)
        
        self.log_symptom_btn = QPushButton("📝 Log Symptom")
        self.log_symptom_btn.setStyleSheet("""
            QPushButton {
                background-color: #FF9800;
                color: white;
                border: none;
                padding: 8px;
                border-radius: 4px;
                font-size: 10px;
            }
            QPushButton:hover {
                background-color: #F57C00;
            }
        """)
        
        self.emergency_btn = QPushButton("🚨 Emergency")
        self.emergency_btn.setStyleSheet("""
            QPushButton {
                background-color: #F44336;
                color: white;
                border: none;
                padding: 8px;
                border-radius: 4px;
                font-size: 10px;
                font-weight: bold;
            }
            QPushButton:hover {
                background-color: #D32F2F;
            }
        """)
        
        actions_layout.addWidget(self.upload_prescription_btn)
        actions_layout.addWidget(self.log_symptom_btn)
        actions_layout.addWidget(self.emergency_btn)
        layout.addLayout(actions_layout)
        
        # Connect buttons
        self.upload_prescription_btn.clicked.connect(self.trigger_prescription_upload)
        self.log_symptom_btn.clicked.connect(self.trigger_symptom_log)
        self.emergency_btn.clicked.connect(self.trigger_emergency)
        
        # Initial status update
        self.update_status()
    
    def update_status(self):
        """Update healthcare status display"""
        try:
            # Get patient info
            patient = self.healthcare_db.get_patient(1)
            if patient:
                week = patient.get('gestational_week', 20)
                self.pregnancy_status.setText(f"📅 Week {week} • Second Trimester")
            
            # Get today's medication reminders
            reminders = self.healthcare_db.get_active_reminders(1)
            if reminders:
                active_today = [r for r in reminders if self._is_reminder_for_today(r)]
                if active_today:
                    self.medication_status.setText(f"💊 {len(active_today)} medication reminders today")
                    self.medication_status.setStyleSheet("""
                        QLabel {
                            color: #FF9800;
                            font-size: 12px;
                            padding: 6px;
                            background-color: rgba(255, 152, 0, 0.1);
                            border-radius: 4px;
                            border: 1px solid #FF9800;
                        }
                    """)
                else:
                    self.medication_status.setText("💊 No medication reminders today")
            
        except Exception as e:
            print(f"Error updating healthcare status: {e}")
    
    def _is_reminder_for_today(self, reminder: Dict[str, Any]) -> bool:
        """Check if reminder is for today"""
        try:
            today = datetime.now().strftime("%Y-%m-%d")
            start_date = datetime.strptime(reminder['start_date'], "%Y-%m-%d").date()
            end_date = datetime.strptime(reminder['end_date'], "%Y-%m-%d").date()
            today_date = datetime.strptime(today, "%Y-%m-%d").date()
            
            return start_date <= today_date <= end_date
        except:
            return False
    
    def trigger_prescription_upload(self):
        """Trigger prescription upload via voice command simulation"""
        try:
            with open(TempDirectoryPath('Responses.data'), 'w', encoding='utf-8') as file:
                file.write("Healthcare: Please show your prescription to the camera or upload the image file.")
        except Exception as e:
            print(f"Error triggering prescription upload: {e}")
    
    def trigger_symptom_log(self):
        """Trigger symptom logging"""
        try:
            with open(TempDirectoryPath('Responses.data'), 'w', encoding='utf-8') as file:
                file.write("Healthcare: Please describe your symptom, and I'll log it for you.")
        except Exception as e:
            print(f"Error triggering symptom log: {e}")
    
    def trigger_emergency(self):
        """Trigger emergency protocol"""
        try:
            with open(TempDirectoryPath('Responses.data'), 'w', encoding='utf-8') as file:
                file.write("Healthcare: Emergency protocol activated. Contacting your healthcare provider immediately.")
        except Exception as e:
            print(f"Error triggering emergency: {e}")

class MedicationReminderWidget(QWidget):
    """
    Widget for displaying and managing medication reminders
    """
    
    def __init__(self):
        super().__init__()
        self.healthcare_db = HealthcareDatabase()
        self.setup_ui()
        
        # Update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.refresh_reminders)
        self.update_timer.start(60000)  # Update every minute
    
    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(5, 5, 5, 5)
        
        # Header
        header = QLabel("💊 Today's Medications")
        header.setStyleSheet("""
            QLabel {
                color: #FF9800;
                font-size: 14px;
                font-weight: bold;
                padding: 8px;
                background-color: rgba(255, 152, 0, 0.1);
                border-radius: 6px;
                border: 1px solid #FF9800;
            }
        """)
        layout.addWidget(header)
        
        # Medication list
        self.medication_list = QListWidget()
        self.medication_list.setStyleSheet("""
            QListWidget {
                background-color: rgba(0, 0, 0, 0.3);
                border: 1px solid #666;
                border-radius: 4px;
                color: white;
                font-size: 11px;
            }
            QListWidgetItem {
                padding: 8px;
                border-bottom: 1px solid #444;
            }
            QListWidgetItem:selected {
                background-color: rgba(76, 175, 80, 0.3);
            }
        """)
        layout.addWidget(self.medication_list)
        
        # Add medication button
        self.add_med_btn = QPushButton("➕ Add Medication Reminder")
        self.add_med_btn.setStyleSheet("""
            QPushButton {
                background-color: #4CAF50;
                color: white;
                border: none;
                padding: 10px;
                border-radius: 4px;
                font-size: 11px;
            }
            QPushButton:hover {
                background-color: #45a049;
            }
        """)
        self.add_med_btn.clicked.connect(self.add_medication_reminder)
        layout.addWidget(self.add_med_btn)
        
        # Initial load
        self.refresh_reminders()
    
    def refresh_reminders(self):
        """Refresh the medication reminders list"""
        try:
            self.medication_list.clear()
            
            # Get today's reminders
            reminders = self.healthcare_db.get_active_reminders(1)
            today_reminders = [r for r in reminders if self._is_reminder_for_today(r)]
            
            if not today_reminders:
                item = QListWidgetItem("No medication reminders for today")
                item.setForeground(QColor('#888'))
                self.medication_list.addItem(item)
                return
            
            for reminder in today_reminders:
                med_name = reminder['medication_name']
                dosage = reminder['dosage']
                times = reminder['times'] if isinstance(reminder['times'], list) else []
                
                for time_str in times:
                    item_text = f"🕐 {time_str} - {med_name} ({dosage})"
                    item = QListWidgetItem(item_text)
                    
                    # Color code based on time
                    current_time = datetime.now().strftime("%H:%M")
                    if current_time >= time_str:
                        item.setForeground(QColor('#4CAF50'))  # Green for past times
                    else:
                        item.setForeground(QColor('#FFF'))     # White for upcoming
                    
                    self.medication_list.addItem(item)
            
        except Exception as e:
            print(f"Error refreshing medication reminders: {e}")
            error_item = QListWidgetItem("Error loading reminders")
            error_item.setForeground(QColor('#F44336'))
            self.medication_list.addItem(error_item)
    
    def _is_reminder_for_today(self, reminder: Dict[str, Any]) -> bool:
        """Check if reminder is for today"""
        try:
            today = datetime.now().strftime("%Y-%m-%d")
            start_date = datetime.strptime(reminder['start_date'], "%Y-%m-%d").date()
            end_date = datetime.strptime(reminder['end_date'], "%Y-%m-%d").date()
            today_date = datetime.strptime(today, "%Y-%m-%d").date()
            
            return start_date <= today_date <= end_date
        except:
            return False
    
    def add_medication_reminder(self):
        """Add a new medication reminder"""
        try:
            with open(TempDirectoryPath('Responses.data'), 'w', encoding='utf-8') as file:
                file.write("Healthcare: Say 'remind me to take [medication name]' to set up a new medication reminder.")
        except Exception as e:
            print(f"Error adding medication reminder: {e}")

class PregnancyTrackingWidget(QWidget):
    """
    Widget for pregnancy tracking and milestones
    """
    
    def __init__(self):
        super().__init__()
        self.healthcare_db = HealthcareDatabase()
        self.setup_ui()
    
    def setup_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(5, 5, 5, 5)
        
        # Header
        header = QLabel("🤱 Pregnancy Tracking")
        header.setStyleSheet("""
            QLabel {
                color: #E91E63;
                font-size: 14px;
                font-weight: bold;
                padding: 8px;
                background-color: rgba(233, 30, 99, 0.1);
                border-radius: 6px;
                border: 1px solid #E91E63;
            }
        """)
        layout.addWidget(header)
        
        # Progress bar for pregnancy weeks
        progress_label = QLabel("Pregnancy Progress")
        progress_label.setStyleSheet("color: white; font-size: 11px; margin-top: 10px;")
        layout.addWidget(progress_label)
        
        self.pregnancy_progress = QProgressBar()
        self.pregnancy_progress.setMinimum(0)
        self.pregnancy_progress.setMaximum(40)  # 40 weeks
        self.pregnancy_progress.setValue(20)    # Current week
        self.pregnancy_progress.setStyleSheet("""
            QProgressBar {
                border: 2px solid #E91E63;
                border-radius: 8px;
                text-align: center;
                background-color: rgba(233, 30, 99, 0.1);
                color: white;
                font-size: 10px;
                font-weight: bold;
            }
            QProgressBar::chunk {
                background-color: #E91E63;
                border-radius: 6px;
            }
        """)
        self.pregnancy_progress.setFormat("Week %v of %m")
        layout.addWidget(self.pregnancy_progress)
        
        # Current week info
        self.week_info = QLabel("🌱 Baby is developing rapidly!\nOrgan formation is complete.")
        self.week_info.setStyleSheet("""
            QLabel {
                color: white;
                font-size: 10px;
                padding: 8px;
                background-color: rgba(233, 30, 99, 0.1);
                border-radius: 4px;
                border: 1px solid #E91E63;
                margin-top: 5px;
            }
        """)
        self.week_info.setWordWrap(True)
        layout.addWidget(self.week_info)
        
        # Next appointment
        self.next_appointment = QLabel("📅 Next Appointment: Tuesday, 2 PM")
        self.next_appointment.setStyleSheet("""
            QLabel {
                color: #2196F3;
                font-size: 11px;
                padding: 6px;
                background-color: rgba(33, 150, 243, 0.1);
                border-radius: 4px;
                border: 1px solid #2196F3;
                margin-top: 5px;
            }
        """)
        layout.addWidget(self.next_appointment)
        
        # Spacer
        layout.addItem(QSpacerItem(20, 40, QSizePolicy.Minimum, QSizePolicy.Expanding))

class HealthcareTabWidget(QTabWidget):
    """
    Main healthcare tab widget for integration with existing GUI
    """
    
    def __init__(self):
        super().__init__()
        self.setup_tabs()
    
    def setup_tabs(self):
        # Set tab bar style
        self.setStyleSheet("""
            QTabWidget::pane {
                border: 1px solid #444;
                background-color: rgba(0, 0, 0, 0.7);
            }
            QTabBar::tab {
                background-color: rgba(0, 0, 0, 0.5);
                color: white;
                padding: 8px 12px;
                margin: 2px;
                border-radius: 4px;
                font-size: 10px;
            }
            QTabBar::tab:selected {
                background-color: #4CAF50;
                font-weight: bold;
            }
            QTabBar::tab:hover {
                background-color: rgba(76, 175, 80, 0.3);
            }
        """)
        
        # Dashboard tab
        dashboard_widget = QWidget()
        dashboard_layout = QVBoxLayout(dashboard_widget)
        dashboard_layout.addWidget(HealthcareStatusWidget())
        self.addTab(dashboard_widget, "🏥 Dashboard")
        
        # Medications tab
        medications_widget = QWidget()
        medications_layout = QVBoxLayout(medications_widget)
        medications_layout.addWidget(MedicationReminderWidget())
        self.addTab(medications_widget, "💊 Medications")
        
        # Pregnancy tab
        pregnancy_widget = QWidget()
        pregnancy_layout = QVBoxLayout(pregnancy_widget)
        pregnancy_layout.addWidget(PregnancyTrackingWidget())
        self.addTab(pregnancy_widget, "🤱 Pregnancy")

# Integration with existing J.A.R.V.I.S. GUI
def create_healthcare_overlay() -> HealthcareTabWidget:
    """Create healthcare overlay widget for integration"""
    return HealthcareTabWidget()

# Function to add healthcare panel to existing GUI
def integrate_healthcare_with_existing_gui(parent_widget):
    """
    Integrate healthcare panel with existing J.A.R.V.I.S. GUI
    This function can be called from the main GUI module
    """
    try:
        healthcare_widget = create_healthcare_overlay()
        healthcare_widget.setMaximumWidth(350)  # Sidebar width
        healthcare_widget.setMaximumHeight(600) # Sidebar height
        
        # Add to parent layout if it exists
        if hasattr(parent_widget, 'layout') and parent_widget.layout():
            parent_widget.layout().addWidget(healthcare_widget)
        
        print("✅ Healthcare GUI overlay integrated successfully")
        return healthcare_widget
        
    except Exception as e:
        print(f"Error integrating healthcare GUI: {e}")
        return None