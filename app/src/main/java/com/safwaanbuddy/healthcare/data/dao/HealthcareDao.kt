package com.safwaanbuddy.healthcare.data.dao

import androidx.room.*
import com.safwaanbuddy.healthcare.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Patient Data Access Object
 * Handles all patient-related database operations
 */
@Dao
interface PatientDao {
    
    @Query("SELECT * FROM patients WHERE is_active = 1 ORDER BY created_at DESC")
    fun getAllActivePatients(): Flow<List<Patient>>
    
    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientById(patientId: Long): Patient?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long
    
    @Update
    suspend fun updatePatient(patient: Patient)
    
    @Query("UPDATE patients SET is_active = 0 WHERE id = :patientId")
    suspend fun deactivatePatient(patientId: Long)
    
    @Query("SELECT COUNT(*) FROM patients WHERE is_active = 1")
    suspend fun getActivePatientCount(): Int
}

/**
 * Prescription Data Access Object
 * Handles prescription and medication parsing operations
 */
@Dao
interface PrescriptionDao {
    
    @Query("SELECT * FROM prescriptions WHERE patient_id = :patientId AND is_active = 1 ORDER BY created_at DESC")
    fun getPrescriptionsByPatient(patientId: Long): Flow<List<Prescription>>
    
    @Query("SELECT * FROM prescriptions WHERE verification_status = :status ORDER BY created_at ASC")
    fun getPrescriptionsByStatus(status: String): Flow<List<Prescription>>
    
    @Query("SELECT * FROM prescriptions WHERE id = :prescriptionId")
    suspend fun getPrescriptionById(prescriptionId: Long): Prescription?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription): Long
    
    @Update
    suspend fun updatePrescription(prescription: Prescription)
    
    @Query("UPDATE prescriptions SET verification_status = :status, clinician_verified = :verified, updated_at = :updatedAt WHERE id = :prescriptionId")
    suspend fun updateVerificationStatus(prescriptionId: Long, status: String, verified: Boolean, updatedAt: LocalDateTime)
    
    @Query("DELETE FROM prescriptions WHERE id = :prescriptionId")
    suspend fun deletePrescription(prescriptionId: Long)
}

/**
 * Medication Reminder Data Access Object
 * Handles medication scheduling and compliance tracking
 */
@Dao
interface MedicationReminderDao {
    
    @Query("SELECT * FROM medication_reminders WHERE patient_id = :patientId AND is_active = 1 ORDER BY next_reminder ASC")
    fun getActiveRemindersByPatient(patientId: Long): Flow<List<MedicationReminder>>
    
    @Query("SELECT * FROM medication_reminders WHERE next_reminder <= :currentTime AND is_active = 1 ORDER BY next_reminder ASC")
    suspend fun getDueReminders(currentTime: LocalDateTime): List<MedicationReminder>
    
    @Query("SELECT * FROM medication_reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): MedicationReminder?
    
    @Query("SELECT * FROM medication_reminders WHERE prescription_id = :prescriptionId AND is_active = 1")
    fun getRemindersByPrescription(prescriptionId: Long): Flow<List<MedicationReminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long
    
    @Update
    suspend fun updateReminder(reminder: MedicationReminder)
    
    @Query("UPDATE medication_reminders SET next_reminder = :nextReminder, updated_at = :updatedAt WHERE id = :reminderId")
    suspend fun updateNextReminder(reminderId: Long, nextReminder: LocalDateTime, updatedAt: LocalDateTime)
    
    @Query("UPDATE medication_reminders SET is_active = 0, updated_at = :updatedAt WHERE id = :reminderId")
    suspend fun deactivateReminder(reminderId: Long, updatedAt: LocalDateTime)
    
    @Query("SELECT COUNT(*) FROM medication_reminders WHERE patient_id = :patientId AND is_active = 1")
    suspend fun getActiveReminderCount(patientId: Long): Int
}

/**
 * Lab Results Data Access Object
 * Handles laboratory test results and analysis
 */
@Dao
interface LabResultDao {
    
    @Query("SELECT * FROM lab_results WHERE patient_id = :patientId ORDER BY test_date DESC")
    fun getLabResultsByPatient(patientId: Long): Flow<List<LabResult>>
    
    @Query("SELECT * FROM lab_results WHERE urgency_level = :urgencyLevel AND clinician_notified = 0 ORDER BY test_date ASC")
    suspend fun getUnnotifiedCriticalResults(urgencyLevel: String = "critical"): List<LabResult>
    
    @Query("SELECT * FROM lab_results WHERE id = :resultId")
    suspend fun getLabResultById(resultId: Long): LabResult?
    
    @Query("SELECT * FROM lab_results WHERE patient_id = :patientId AND test_type = :testType ORDER BY test_date DESC LIMIT 1")
    suspend fun getLatestResultByType(patientId: Long, testType: String): LabResult?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabResult(labResult: LabResult): Long
    
    @Update
    suspend fun updateLabResult(labResult: LabResult)
    
    @Query("UPDATE lab_results SET clinician_notified = 1, updated_at = :updatedAt WHERE id = :resultId")
    suspend fun markClinicianNotified(resultId: Long, updatedAt: LocalDateTime)
    
    @Query("UPDATE lab_results SET patient_notified = 1, updated_at = :updatedAt WHERE id = :resultId")
    suspend fun markPatientNotified(resultId: Long, updatedAt: LocalDateTime)
    
    @Query("SELECT * FROM lab_results WHERE test_date BETWEEN :startDate AND :endDate AND patient_id = :patientId ORDER BY test_date DESC")
    fun getLabResultsByDateRange(patientId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<LabResult>>
}

/**
 * Voice Command Log Data Access Object
 * Handles voice interaction logging for compliance and analysis
 */
@Dao
interface VoiceCommandLogDao {
    
    @Query("SELECT * FROM voice_command_logs WHERE patient_id = :patientId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCommandsByPatient(patientId: Long, limit: Int = 50): Flow<List<VoiceCommandLog>>
    
    @Query("SELECT * FROM voice_command_logs WHERE intent_classification = :intent ORDER BY timestamp DESC")
    fun getCommandsByIntent(intent: String): Flow<List<VoiceCommandLog>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceCommand(voiceCommand: VoiceCommandLog): Long
    
    @Query("SELECT AVG(confidence_score) FROM voice_command_logs WHERE patient_id = :patientId AND timestamp >= :since")
    suspend fun getAverageConfidenceScore(patientId: Long, since: LocalDateTime): Float?
    
    @Query("SELECT COUNT(*) FROM voice_command_logs WHERE was_successful = 1 AND patient_id = :patientId AND timestamp >= :since")
    suspend fun getSuccessfulCommandCount(patientId: Long, since: LocalDateTime): Int
    
    @Query("DELETE FROM voice_command_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: LocalDateTime)
}

/**
 * Medication Compliance Data Access Object
 * Tracks medication adherence and compliance metrics
 */
@Dao
interface MedicationComplianceDao {
    
    @Query("SELECT * FROM medication_compliance WHERE patient_id = :patientId ORDER BY scheduled_time DESC")
    fun getComplianceByPatient(patientId: Long): Flow<List<MedicationCompliance>>
    
    @Query("SELECT * FROM medication_compliance WHERE medication_reminder_id = :reminderId ORDER BY scheduled_time DESC")
    fun getComplianceByReminder(reminderId: Long): Flow<List<MedicationCompliance>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompliance(compliance: MedicationCompliance): Long
    
    @Query("""
        SELECT COUNT(*) * 100.0 / (
            SELECT COUNT(*) FROM medication_compliance 
            WHERE patient_id = :patientId AND scheduled_time >= :startDate AND scheduled_time <= :endDate
        ) as compliance_percentage
        FROM medication_compliance 
        WHERE patient_id = :patientId 
        AND status = 'taken' 
        AND scheduled_time >= :startDate 
        AND scheduled_time <= :endDate
    """)
    suspend fun getCompliancePercentage(patientId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Float?
    
    @Query("SELECT * FROM medication_compliance WHERE status = 'missed' AND patient_id = :patientId ORDER BY scheduled_time DESC LIMIT :limit")
    suspend fun getRecentMissedDoses(patientId: Long, limit: Int = 10): List<MedicationCompliance>
}

/**
 * Emergency Contact Data Access Object
 * Manages emergency contact information
 */
@Dao
interface EmergencyContactDao {
    
    @Query("SELECT * FROM emergency_contacts WHERE patient_id = :patientId AND is_active = 1 ORDER BY is_primary DESC, relationship ASC")
    fun getContactsByPatient(patientId: Long): Flow<List<EmergencyContact>>
    
    @Query("SELECT * FROM emergency_contacts WHERE patient_id = :patientId AND is_primary = 1 AND is_active = 1 LIMIT 1")
    suspend fun getPrimaryContact(patientId: Long): EmergencyContact?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact): Long
    
    @Update
    suspend fun updateContact(contact: EmergencyContact)
    
    @Query("UPDATE emergency_contacts SET is_active = 0, updated_at = :updatedAt WHERE id = :contactId")
    suspend fun deactivateContact(contactId: Long, updatedAt: LocalDateTime)
    
    @Query("UPDATE emergency_contacts SET is_primary = 0, updated_at = :updatedAt WHERE patient_id = :patientId AND id != :newPrimaryId")
    suspend fun clearPrimaryStatus(patientId: Long, newPrimaryId: Long, updatedAt: LocalDateTime)
}

/**
 * Health Metrics Data Access Object
 * Tracks vital signs and health measurements
 */
@Dao
interface HealthMetricDao {
    
    @Query("SELECT * FROM health_metrics WHERE patient_id = :patientId ORDER BY measured_at DESC")
    fun getMetricsByPatient(patientId: Long): Flow<List<HealthMetric>>
    
    @Query("SELECT * FROM health_metrics WHERE patient_id = :patientId AND metric_type = :metricType ORDER BY measured_at DESC LIMIT :limit")
    fun getMetricsByType(patientId: Long, metricType: String, limit: Int = 30): Flow<List<HealthMetric>>
    
    @Query("SELECT * FROM health_metrics WHERE is_flagged = 1 AND patient_id = :patientId ORDER BY measured_at DESC")
    fun getFlaggedMetrics(patientId: Long): Flow<List<HealthMetric>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: HealthMetric): Long
    
    @Update
    suspend fun updateMetric(metric: HealthMetric)
    
    @Query("SELECT * FROM health_metrics WHERE patient_id = :patientId AND metric_type = :metricType ORDER BY measured_at DESC LIMIT 1")
    suspend fun getLatestMetric(patientId: Long, metricType: String): HealthMetric?
    
    @Query("DELETE FROM health_metrics WHERE id = :metricId")
    suspend fun deleteMetric(metricId: Long)
}