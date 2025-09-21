package com.safwaanbuddy.healthcare.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.safwaanbuddy.healthcare.data.converters.DateConverters
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Patient Profile Entity for Healthcare Management
 * Contains encrypted sensitive medical information
 */
@Entity(tableName = "patients")
@TypeConverters(DateConverters::class)
@Parcelize
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name_encrypted")
    val nameEncrypted: String, // Encrypted patient name
    
    @ColumnInfo(name = "date_of_birth_encrypted")
    val dateOfBirthEncrypted: String, // Encrypted birth date
    
    @ColumnInfo(name = "expected_due_date")
    val expectedDueDate: LocalDate?, // Pregnancy due date
    
    @ColumnInfo(name = "gestational_week")
    val gestationalWeek: Int = 0, // Current pregnancy week
    
    @ColumnInfo(name = "allergies_encrypted")
    val allergiesEncrypted: String?, // Encrypted medical allergies
    
    @ColumnInfo(name = "emergency_contact_encrypted")
    val emergencyContactEncrypted: String?, // Encrypted emergency contact
    
    @ColumnInfo(name = "medical_conditions_encrypted")
    val medicalConditionsEncrypted: String?, // Encrypted pre-existing conditions
    
    @ColumnInfo(name = "blood_type")
    val bloodType: String?, // Blood type (not encrypted, emergency info)
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
) : Parcelable

/**
 * Prescription Entity for Medication Management
 * Stores prescription images and parsed medication data
 */
@Entity(tableName = "prescriptions")
@TypeConverters(DateConverters::class)
@Parcelize
data class Prescription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "image_path_encrypted")
    val imagePathEncrypted: String, // Encrypted prescription image path
    
    @ColumnInfo(name = "ocr_text_encrypted")
    val ocrTextEncrypted: String, // Encrypted extracted text
    
    @ColumnInfo(name = "parsed_medications_encrypted")
    val parsedMedicationsEncrypted: String, // Encrypted parsed medication JSON
    
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String = "pending", // pending, verified, rejected
    
    @ColumnInfo(name = "clinician_verified")
    val clinicianVerified: Boolean = false,
    
    @ColumnInfo(name = "prescriber_name_encrypted")
    val prescriberNameEncrypted: String?, // Encrypted doctor name
    
    @ColumnInfo(name = "prescription_date")
    val prescriptionDate: LocalDate?,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
) : Parcelable

/**
 * Enhanced Medication Reminder Entity
 * Manages medication schedules with compliance tracking
 */
@Entity(tableName = "medication_reminders")
@TypeConverters(DateConverters::class)
@Parcelize
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "prescription_id")
    val prescriptionId: Long?,
    
    @ColumnInfo(name = "medication_name")
    val medicationName: String,
    
    @ColumnInfo(name = "dosage")
    val dosage: String,
    
    @ColumnInfo(name = "frequency")
    val frequency: String, // e.g., "twice daily", "every 8 hours"
    
    @ColumnInfo(name = "scheduled_times")
    val scheduledTimes: String, // JSON array of times: ["08:00", "20:00"]
    
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,
    
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate?,
    
    @ColumnInfo(name = "instructions")
    val instructions: String?, // e.g., "Take with food"
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "snooze_count")
    val snoozeCount: Int = 0,
    
    @ColumnInfo(name = "compliance_log_encrypted")
    val complianceLogEncrypted: String?, // Encrypted medication compliance history JSON
    
    @ColumnInfo(name = "pregnancy_safety_category")
    val pregnancySafetyCategory: String?, // safe, caution, avoid, unknown
    
    @ColumnInfo(name = "last_taken")
    val lastTaken: LocalDateTime?,
    
    @ColumnInfo(name = "next_reminder")
    val nextReminder: LocalDateTime?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
) : Parcelable

/**
 * Lab Results Entity for Medical Test Management
 * Stores laboratory test results with emergency flagging
 */
@Entity(tableName = "lab_results")
@TypeConverters(DateConverters::class)
@Parcelize
data class LabResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "test_date")
    val testDate: LocalDate,
    
    @ColumnInfo(name = "test_type")
    val testType: String, // e.g., "Blood Work", "Prenatal Screening"
    
    @ColumnInfo(name = "results_encrypted")
    val resultsEncrypted: String, // Encrypted test results JSON
    
    @ColumnInfo(name = "flagged_values_encrypted")
    val flaggedValuesEncrypted: String?, // Encrypted abnormal values JSON
    
    @ColumnInfo(name = "urgency_level")
    val urgencyLevel: String, // normal, elevated, critical
    
    @ColumnInfo(name = "clinician_notified")
    val clinicianNotified: Boolean = false,
    
    @ColumnInfo(name = "patient_notified")
    val patientNotified: Boolean = false,
    
    @ColumnInfo(name = "provider_name_encrypted")
    val providerNameEncrypted: String?, // Encrypted healthcare provider name
    
    @ColumnInfo(name = "lab_facility_encrypted")
    val labFacilityEncrypted: String?, // Encrypted lab facility name
    
    @ColumnInfo(name = "notes_encrypted")
    val notesEncrypted: String?, // Encrypted provider notes
    
    @ColumnInfo(name = "follow_up_required")
    val followUpRequired: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
) : Parcelable

/**
 * Voice Command Log Entity
 * Tracks healthcare voice interactions for compliance and improvement
 */
@Entity(tableName = "voice_command_logs")
@TypeConverters(DateConverters::class)
@Parcelize
data class VoiceCommandLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long?,
    
    @ColumnInfo(name = "command_text")
    val commandText: String, // Transcribed voice command
    
    @ColumnInfo(name = "intent_classification")
    val intentClassification: String, // AI-classified user intent
    
    @ColumnInfo(name = "response_generated")
    val responseGenerated: String, // System response
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float, // Recognition confidence (0.0 - 1.0)
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long, // Time taken to process command
    
    @ColumnInfo(name = "was_successful")
    val wasSuccessful: Boolean,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: LocalDateTime
) : Parcelable

/**
 * Medication Compliance Log Entry
 * Tracks individual medication doses taken/missed
 */
@Entity(tableName = "medication_compliance")
@TypeConverters(DateConverters::class)
@Parcelize
data class MedicationCompliance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "medication_reminder_id")
    val medicationReminderId: Long,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: LocalDateTime,
    
    @ColumnInfo(name = "actual_time")
    val actualTime: LocalDateTime?,
    
    @ColumnInfo(name = "status")
    val status: String, // taken, missed, snoozed, skipped
    
    @ColumnInfo(name = "delay_minutes")
    val delayMinutes: Int = 0, // Minutes late if taken after scheduled time
    
    @ColumnInfo(name = "notes")
    val notes: String?, // Optional patient notes
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime
) : Parcelable

/**
 * Emergency Contact Entity
 * Stores encrypted emergency contact information
 */
@Entity(tableName = "emergency_contacts")
@TypeConverters(DateConverters::class)
@Parcelize
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "name_encrypted")
    val nameEncrypted: String, // Encrypted contact name
    
    @ColumnInfo(name = "relationship")
    val relationship: String, // spouse, parent, sibling, friend, etc.
    
    @ColumnInfo(name = "phone_encrypted")
    val phoneEncrypted: String, // Encrypted phone number
    
    @ColumnInfo(name = "email_encrypted")
    val emailEncrypted: String?, // Encrypted email address
    
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime
) : Parcelable

/**
 * Health Metric Entity
 * Tracks vital signs and health metrics over time
 */
@Entity(tableName = "health_metrics")
@TypeConverters(DateConverters::class)
@Parcelize
data class HealthMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "patient_id")
    val patientId: Long,
    
    @ColumnInfo(name = "metric_type")
    val metricType: String, // blood_pressure, weight, temperature, heart_rate, etc.
    
    @ColumnInfo(name = "value")
    val value: String, // Metric value (could be "120/80" for BP, "150.5" for weight)
    
    @ColumnInfo(name = "unit")
    val unit: String, // mmHg, lbs, °F, bpm, etc.
    
    @ColumnInfo(name = "measured_at")
    val measuredAt: LocalDateTime,
    
    @ColumnInfo(name = "notes")
    val notes: String?,
    
    @ColumnInfo(name = "is_flagged")
    val isFlagged: Boolean = false, // True if value is outside normal range
    
    @ColumnInfo(name = "source")
    val source: String, // manual, device, imported
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime
) : Parcelable