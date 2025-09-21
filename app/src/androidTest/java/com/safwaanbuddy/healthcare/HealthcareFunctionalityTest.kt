package com.safwaanbuddy.healthcare

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.models.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive Healthcare Functionality Test Suite
 * Tests all core healthcare components including encryption, database, and business logic
 */
@RunWith(AndroidJUnit4::class)
class HealthcareFunctionalityTest {
    
    private lateinit var database: HealthcareDatabase
    private lateinit var encryptionService: HealthcareEncryptionService
    
    @Before
    fun setUp() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HealthcareDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Initialize encryption service
        encryptionService = HealthcareEncryptionService(ApplicationProvider.getApplicationContext())
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testDatabaseEncryption() = runBlocking {
        // Test data encryption and decryption
        val originalText = "Sensitive Patient Information"
        val patientId = "12345"
        
        val encrypted = encryptionService.encrypt(originalText, "test_context_$patientId")
        assertNotEquals("Encrypted text should be different from original", originalText, encrypted)
        
        val decrypted = encryptionService.decrypt(encrypted, "test_context_$patientId")
        assertEquals("Decrypted text should match original", originalText, decrypted)
    }
    
    @Test
    fun testPatientDataStorage() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient with encrypted data
        val encryptedName = encryptionService.encrypt("Jane Doe", "patient_name_1")
        val encryptedDob = encryptionService.encrypt("1990-01-01", "patient_dob_1")
        val encryptedAllergies = encryptionService.encrypt("Penicillin", "patient_allergies_1")
        
        val patient = Patient(
            nameEncrypted = encryptedName,
            dateOfBirthEncrypted = encryptedDob,
            expectedDueDate = null,
            gestationalWeek = 20,
            allergiesEncrypted = encryptedAllergies,
            emergencyContactEncrypted = null,
            bloodType = "O+",
            createdAt = now,
            updatedAt = now
        )
        
        // Insert patient
        val patientId = database.patientDao().insertPatient(patient)
        assertTrue("Patient ID should be positive", patientId > 0)
        
        // Retrieve and verify
        val retrievedPatient = database.patientDao().getPatientById(patientId)
        assertNotNull("Retrieved patient should not be null", retrievedPatient)
        assertEquals("Blood type should match", "O+", retrievedPatient?.bloodType)
        
        // Verify encryption
        val decryptedName = encryptionService.decrypt(retrievedPatient!!.nameEncrypted, "patient_name_1")
        assertEquals("Decrypted name should match", "Jane Doe", decryptedName)
    }
    
    @Test
    fun testPrescriptionProcessing() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient first
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create test prescription with encrypted data
        val mockPrescriptionText = "Prenatal Vitamin 1 tablet daily"
        val mockMedicationData = """{"medications":[{"name":"Prenatal Vitamin","dosage":"1 tablet","frequency":"daily"}]}"""
        
        val encryptedImagePath = encryptionService.encrypt("/test/prescription.jpg", "prescription_image_$patientId")
        val encryptedOcrText = encryptionService.encrypt(mockPrescriptionText, "prescription_ocr_$patientId")
        val encryptedMedications = encryptionService.encrypt(mockMedicationData, "prescription_data_$patientId")
        
        val prescription = Prescription(
            patientId = patientId,
            imagePathEncrypted = encryptedImagePath,
            ocrTextEncrypted = encryptedOcrText,
            parsedMedicationsEncrypted = encryptedMedications,
            verificationStatus = "pending",
            clinicianVerified = false,
            prescriberNameEncrypted = null,
            prescriptionDate = now.date,
            createdAt = now,
            updatedAt = now
        )
        
        // Insert prescription
        val prescriptionId = database.prescriptionDao().insertPrescription(prescription)
        assertTrue("Prescription ID should be positive", prescriptionId > 0)
        
        // Retrieve and verify
        val retrievedPrescription = database.prescriptionDao().getPrescriptionById(prescriptionId)
        assertNotNull("Retrieved prescription should not be null", retrievedPrescription)
        assertEquals("Verification status should match", "pending", retrievedPrescription?.verificationStatus)
        
        // Test decryption
        val decryptedText = encryptionService.decrypt(retrievedPrescription!!.ocrTextEncrypted, "prescription_ocr_$patientId")
        assertEquals("Decrypted OCR text should match", mockPrescriptionText, decryptedText)
    }
    
    @Test
    fun testMedicationReminderSystem() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create medication reminder
        val reminder = MedicationReminder(
            patientId = patientId,
            prescriptionId = null,
            medicationName = "Prenatal Vitamin",
            dosage = "1 tablet",
            frequency = "once daily",
            scheduledTimes = "08:00",
            startDate = now.date,
            endDate = null,
            instructions = "Take with food",
            pregnancySafetyCategory = "safe",
            nextReminder = now.date.atTime(8, 0),
            createdAt = now,
            updatedAt = now
        )
        
        // Insert reminder
        val reminderId = database.medicationReminderDao().insertReminder(reminder)
        assertTrue("Reminder ID should be positive", reminderId > 0)
        
        // Test retrieval
        val activeReminders = database.medicationReminderDao()
            .getActiveRemindersByPatient(patientId)
            .first()
        
        assertEquals("Should have one active reminder", 1, activeReminders.size)
        assertEquals("Medication name should match", "Prenatal Vitamin", activeReminders[0].medicationName)
        assertEquals("Safety category should match", "safe", activeReminders[0].pregnancySafetyCategory)
    }
    
    @Test
    fun testLabResultsAnalysis() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create test lab results with critical values
        val criticalResults = """{"glucose_fasting": 200, "blood_pressure_systolic": 160}"""
        val flaggedValues = """{"glucose_fasting": "Above Normal (200 > 95)", "blood_pressure_systolic": "Above Normal (160 > 140)"}"""
        
        val encryptedResults = encryptionService.encrypt(criticalResults, "lab_results_$patientId")
        val encryptedFlagged = encryptionService.encrypt(flaggedValues, "flagged_values_$patientId")
        
        val labResult = LabResult(
            patientId = patientId,
            testDate = now.date,
            testType = "Routine Blood Work",
            resultsEncrypted = encryptedResults,
            flaggedValuesEncrypted = encryptedFlagged,
            urgencyLevel = "critical",
            clinicianNotified = false,
            patientNotified = false,
            providerNameEncrypted = null,
            labFacilityEncrypted = null,
            notesEncrypted = null,
            followUpRequired = true,
            createdAt = now,
            updatedAt = now
        )
        
        // Insert lab result
        val resultId = database.labResultDao().insertLabResult(labResult)
        assertTrue("Lab result ID should be positive", resultId > 0)
        
        // Test critical value detection
        val criticalResults = database.labResultDao()
            .getUnnotifiedCriticalResults("critical")
        
        assertEquals("Should have one critical result", 1, criticalResults.size)
        assertEquals("Urgency level should be critical", "critical", criticalResults[0].urgencyLevel)
        assertTrue("Follow-up should be required", criticalResults[0].followUpRequired)
    }
    
    @Test
    fun testVoiceCommandLogging() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create voice command log
        val voiceLog = VoiceCommandLog(
            patientId = patientId,
            commandText = "When is my next medication dose?",
            intentClassification = "MEDICATION_QUERY",
            responseGenerated = "Your next dose of Prenatal Vitamin is at 8:00 AM",
            confidenceScore = 0.95f,
            processingTimeMs = 250L,
            wasSuccessful = true,
            errorMessage = null,
            timestamp = now
        )
        
        // Insert voice log
        val logId = database.voiceCommandLogDao().insertVoiceCommand(voiceLog)
        assertTrue("Voice log ID should be positive", logId > 0)
        
        // Test retrieval
        val recentCommands = database.voiceCommandLogDao()
            .getRecentCommandsByPatient(patientId, 10)
            .first()
        
        assertEquals("Should have one command log", 1, recentCommands.size)
        assertEquals("Command text should match", "When is my next medication dose?", recentCommands[0].commandText)
        assertEquals("Intent should match", "MEDICATION_QUERY", recentCommands[0].intentClassification)
        assertTrue("Confidence should be high", recentCommands[0].confidenceScore > 0.9f)
    }
    
    @Test
    fun testMedicationComplianceTracking() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient and reminder
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        val reminder = createTestMedicationReminder(patientId)
        val reminderId = database.medicationReminderDao().insertReminder(reminder)
        
        // Create compliance records
        val takenCompliance = MedicationCompliance(
            medicationReminderId = reminderId,
            patientId = patientId,
            scheduledTime = now.date.atTime(8, 0),
            actualTime = now.date.atTime(8, 5), // 5 minutes late
            status = "taken",
            delayMinutes = 5,
            notes = "Taken with breakfast",
            createdAt = now
        )
        
        val missedCompliance = MedicationCompliance(
            medicationReminderId = reminderId,
            patientId = patientId,
            scheduledTime = now.date.atTime(20, 0),
            actualTime = null,
            status = "missed",
            delayMinutes = 0,
            notes = null,
            createdAt = now
        )
        
        // Insert compliance records
        database.medicationComplianceDao().insertCompliance(takenCompliance)
        database.medicationComplianceDao().insertCompliance(missedCompliance)
        
        // Test compliance calculation
        val endTime = now.plus(kotlinx.datetime.DateTimeUnit.DAY)
        val startTime = now.minus(kotlinx.datetime.DateTimeUnit.DAY)
        
        val compliancePercentage = database.medicationComplianceDao()
            .getCompliancePercentage(patientId, startTime, endTime)
        
        assertNotNull("Compliance percentage should not be null", compliancePercentage)
        assertEquals("Compliance should be 50%", 50.0f, compliancePercentage!!, 0.1f)
        
        // Test missed doses retrieval
        val missedDoses = database.medicationComplianceDao()
            .getRecentMissedDoses(patientId, 5)
        
        assertEquals("Should have one missed dose", 1, missedDoses.size)
        assertEquals("Status should be missed", "missed", missedDoses[0].status)
    }
    
    @Test
    fun testEmergencyContactManagement() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create emergency contacts with encrypted data
        val encryptedName1 = encryptionService.encrypt("John Doe", "contact_name_$patientId")
        val encryptedPhone1 = encryptionService.encrypt("555-0123", "contact_phone_$patientId")
        
        val primaryContact = EmergencyContact(
            patientId = patientId,
            nameEncrypted = encryptedName1,
            relationship = "spouse",
            phoneEncrypted = encryptedPhone1,
            emailEncrypted = null,
            isPrimary = true,
            createdAt = now,
            updatedAt = now
        )
        
        val encryptedName2 = encryptionService.encrypt("Jane Smith", "contact_name2_$patientId")
        val encryptedPhone2 = encryptionService.encrypt("555-0456", "contact_phone2_$patientId")
        
        val secondaryContact = EmergencyContact(
            patientId = patientId,
            nameEncrypted = encryptedName2,
            relationship = "mother",
            phoneEncrypted = encryptedPhone2,
            emailEncrypted = null,
            isPrimary = false,
            createdAt = now,
            updatedAt = now
        )
        
        // Insert contacts
        database.emergencyContactDao().insertContact(primaryContact)
        database.emergencyContactDao().insertContact(secondaryContact)
        
        // Test primary contact retrieval
        val primary = database.emergencyContactDao().getPrimaryContact(patientId)
        assertNotNull("Primary contact should exist", primary)
        assertTrue("Contact should be primary", primary?.isPrimary ?: false)
        assertEquals("Relationship should match", "spouse", primary?.relationship)
        
        // Test all contacts retrieval
        val allContacts = database.emergencyContactDao()
            .getContactsByPatient(patientId)
            .first()
        
        assertEquals("Should have two contacts", 2, allContacts.size)
        
        // Test decryption
        val decryptedName = encryptionService.decrypt(primary!!.nameEncrypted, "contact_name_$patientId")
        assertEquals("Decrypted name should match", "John Doe", decryptedName)
    }
    
    @Test
    fun testHealthMetricsTracking() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Create test patient
        val patient = createTestPatient()
        val patientId = database.patientDao().insertPatient(patient)
        
        // Create health metrics
        val bloodPressure = HealthMetric(
            patientId = patientId,
            metricType = "blood_pressure",
            value = "120/80",
            unit = "mmHg",
            measuredAt = now,
            notes = "Normal reading",
            isFlagged = false,
            source = "manual",
            createdAt = now
        )
        
        val weight = HealthMetric(
            patientId = patientId,
            metricType = "weight",
            value = "150.5",
            unit = "lbs",
            measuredAt = now,
            notes = "Weekly weigh-in",
            isFlagged = false,
            source = "manual",
            createdAt = now
        )
        
        val criticalBP = HealthMetric(
            patientId = patientId,
            metricType = "blood_pressure",
            value = "160/100",
            unit = "mmHg",
            measuredAt = now.minus(kotlinx.datetime.DateTimeUnit.DAY),
            notes = "Elevated reading",
            isFlagged = true,
            source = "manual",
            createdAt = now.minus(kotlinx.datetime.DateTimeUnit.DAY)
        )
        
        // Insert metrics
        database.healthMetricDao().insertMetric(bloodPressure)
        database.healthMetricDao().insertMetric(weight)
        database.healthMetricDao().insertMetric(criticalBP)
        
        // Test retrieval by type
        val bpMetrics = database.healthMetricDao()
            .getMetricsByType(patientId, "blood_pressure", 10)
            .first()
        
        assertEquals("Should have two BP readings", 2, bpMetrics.size)
        
        // Test flagged metrics
        val flaggedMetrics = database.healthMetricDao()
            .getFlaggedMetrics(patientId)
            .first()
        
        assertEquals("Should have one flagged metric", 1, flaggedMetrics.size)
        assertTrue("Metric should be flagged", flaggedMetrics[0].isFlagged)
        assertEquals("Should be critical BP reading", "160/100", flaggedMetrics[0].value)
        
        // Test latest metric
        val latestBP = database.healthMetricDao()
            .getLatestMetric(patientId, "blood_pressure")
        
        assertNotNull("Latest BP reading should exist", latestBP)
        assertEquals("Should be most recent reading", "120/80", latestBP?.value)
    }
    
    // Helper methods for creating test data
    
    private fun createTestPatient(): Patient {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        return Patient(
            nameEncrypted = encryptionService.encrypt("Test Patient", "patient_name_test"),
            dateOfBirthEncrypted = encryptionService.encrypt("1990-01-01", "patient_dob_test"),
            expectedDueDate = null,
            gestationalWeek = 20,
            allergiesEncrypted = null,
            emergencyContactEncrypted = null,
            bloodType = "O+",
            createdAt = now,
            updatedAt = now
        )
    }
    
    private fun createTestMedicationReminder(patientId: Long): MedicationReminder {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        return MedicationReminder(
            patientId = patientId,
            prescriptionId = null,
            medicationName = "Test Medication",
            dosage = "1 tablet",
            frequency = "twice daily",
            scheduledTimes = "08:00,20:00",
            startDate = now.date,
            endDate = null,
            instructions = "Take with food",
            pregnancySafetyCategory = "safe",
            nextReminder = now.date.atTime(8, 0),
            createdAt = now,
            updatedAt = now
        )
    }
}