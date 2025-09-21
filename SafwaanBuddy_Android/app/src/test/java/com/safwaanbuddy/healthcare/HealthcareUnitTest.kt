package com.safwaanbuddy.healthcare

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionUtils
import com.safwaanbuddy.healthcare.lab.LabResultsAnalyzer
import com.safwaanbuddy.healthcare.lab.UrgencyLevel
import com.safwaanbuddy.healthcare.prescription.MedicationInfo
import com.safwaanbuddy.healthcare.prescription.SafetyAlertLevel
import com.safwaanbuddy.healthcare.reminders.ComplianceStatistics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit Tests for Healthcare Components
 * Tests business logic, calculations, and utilities without Android dependencies
 */
@ExperimentalCoroutinesApi
class HealthcareUnitTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var mockEncryptionService: HealthcareEncryptionService
    
    @Before
    fun setUp() {
        mockEncryptionService = mock()
        
        // Mock encryption/decryption to return predictable values
        whenever(mockEncryptionService.encrypt("test", "context")).thenReturn("encrypted_test")
        whenever(mockEncryptionService.decrypt("encrypted_test", "context")).thenReturn("test")
    }
    
    @Test
    fun testEncryptionUtilities() {
        val patientId = "12345"
        val testName = "John Doe"
        
        // Test patient name encryption utility
        val encryptedName = HealthcareEncryptionUtils.encryptPatientName(
            mockEncryptionService, testName, patientId
        )
        assertEquals("Should return encrypted value", "encrypted_test", encryptedName)
        
        // Test patient name decryption utility
        val decryptedName = HealthcareEncryptionUtils.decryptPatientName(
            mockEncryptionService, "encrypted_test", patientId
        )
        assertEquals("Should return decrypted value", "test", decryptedName)
        
        // Test medical data encryption
        val medicalData = "Sensitive medical information"
        whenever(mockEncryptionService.encrypt(medicalData, "prescription_12345"))
            .thenReturn("encrypted_medical_data")
        
        val encryptedMedical = HealthcareEncryptionUtils.encryptMedicalData(
            mockEncryptionService, medicalData, "prescription", patientId
        )
        assertEquals("Should encrypt medical data", "encrypted_medical_data", encryptedMedical)
        
        // Test validation
        assertTrue("Valid encrypted data should return true", 
            HealthcareEncryptionUtils.isValidEncryptedData("dGVzdCBkYXRh")) // base64 encoded
        assertFalse("Invalid encrypted data should return false", 
            HealthcareEncryptionUtils.isValidEncryptedData("invalid"))
        assertFalse("Null data should return false", 
            HealthcareEncryptionUtils.isValidEncryptedData(null))
    }
    
    @Test
    fun testMedicationSafetyClassification() {
        // Test safe medication
        val safeMedication = MedicationInfo(
            name = "Prenatal Vitamin",
            strength = "1 tablet",
            form = "tablet",
            frequency = "once daily",
            duration = "throughout pregnancy",
            instructions = "Take with food",
            pregnancySafetyCategory = "safe"
        )
        
        assertEquals("Safety category should be safe", "safe", safeMedication.pregnancySafetyCategory)
        
        // Test caution medication
        val cautionMedication = MedicationInfo(
            name = "Ibuprofen",
            strength = "200mg",
            form = "tablet",
            frequency = "as needed",
            duration = "short term",
            instructions = "Take with food",
            pregnancySafetyCategory = "caution"
        )
        
        assertEquals("Safety category should be caution", "caution", cautionMedication.pregnancySafetyCategory)
        
        // Test avoid medication
        val avoidMedication = MedicationInfo(
            name = "Warfarin",
            strength = "5mg",
            form = "tablet",
            frequency = "once daily",
            duration = "as prescribed",
            instructions = "Take at same time daily",
            pregnancySafetyCategory = "avoid"
        )
        
        assertEquals("Safety category should be avoid", "avoid", avoidMedication.pregnancySafetyCategory)
    }
    
    @Test
    fun testLabResultsUrgencyClassification() {
        // Test normal values
        val normalResults = mapOf(
            "glucose_fasting" to 85.0,
            "blood_pressure_systolic" to 120.0,
            "hemoglobin" to 12.5
        )
        
        val normalUrgency = determineLabUrgency(normalResults)
        assertEquals("Normal values should have normal urgency", UrgencyLevel.NORMAL, normalUrgency)
        
        // Test elevated values
        val elevatedResults = mapOf(
            "glucose_fasting" to 105.0, // Slightly elevated
            "hemoglobin" to 10.5 // Slightly low
        )
        
        val elevatedUrgency = determineLabUrgency(elevatedResults)
        assertTrue("Elevated values should be elevated or critical", 
            elevatedUrgency == UrgencyLevel.ELEVATED || elevatedUrgency == UrgencyLevel.CRITICAL)
        
        // Test critical values
        val criticalResults = mapOf(
            "glucose_fasting" to 200.0, // Very high
            "blood_pressure_systolic" to 180.0, // Hypertensive crisis
            "platelets" to 50.0 // Very low
        )
        
        val criticalUrgency = determineLabUrgency(criticalResults)
        assertEquals("Critical values should have critical urgency", UrgencyLevel.CRITICAL, criticalUrgency)
    }
    
    @Test
    fun testMedicationFrequencyParsing() {
        // Test frequency to times conversion
        assertEquals("Once daily should be morning", listOf("08:00"), parseFrequencyToTimes("once daily"))
        assertEquals("Twice daily should be morning and evening", 
            listOf("08:00", "20:00"), parseFrequencyToTimes("twice daily"))
        assertEquals("Three times daily should include afternoon", 
            listOf("08:00", "14:00", "20:00"), parseFrequencyToTimes("three times daily"))
        assertEquals("Every 8 hours should be 8-hour intervals", 
            listOf("08:00", "16:00", "00:00"), parseFrequencyToTimes("every 8 hours"))
        
        // Test edge cases
        assertEquals("Empty frequency should default to morning", 
            listOf("08:00"), parseFrequencyToTimes(""))
        assertEquals("Unknown frequency should default to morning", 
            listOf("08:00"), parseFrequencyToTimes("unknown"))
    }
    
    @Test
    fun testDurationCalculation() {
        val startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        // Test days
        val endDate7Days = calculateEndDate("7 days", startDate)
        assertEquals("7 days should add 7 days", 
            startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * 7), endDate7Days)
        
        // Test weeks
        val endDate2Weeks = calculateEndDate("2 weeks", startDate)
        assertEquals("2 weeks should add 14 days", 
            startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * 14), endDate2Weeks)
        
        // Test months
        val endDate1Month = calculateEndDate("1 month", startDate)
        assertEquals("1 month should add 1 month", 
            startDate.plus(kotlinx.datetime.DateTimeUnit.MONTH), endDate1Month)
        
        // Test invalid duration
        val endDateInvalid = calculateEndDate("invalid", startDate)
        assertEquals("Invalid duration should default to 30 days", 
            startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * 30), endDateInvalid)
    }
    
    @Test
    fun testComplianceCalculations() {
        // Test perfect compliance
        val perfectCompliance = ComplianceStatistics(
            compliancePercentage = 100.0f,
            totalDays = 30,
            recentMissedDoses = 0,
            lastMissedDose = null
        )
        
        assertEquals("Perfect compliance should be 100%", 100.0f, perfectCompliance.compliancePercentage, 0.1f)
        assertEquals("No missed doses", 0, perfectCompliance.recentMissedDoses)
        assertNull("No last missed dose", perfectCompliance.lastMissedDose)
        
        // Test poor compliance
        val poorCompliance = ComplianceStatistics(
            compliancePercentage = 60.0f,
            totalDays = 30,
            recentMissedDoses = 12,
            lastMissedDose = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
        
        assertEquals("Poor compliance should be 60%", 60.0f, poorCompliance.compliancePercentage, 0.1f)
        assertEquals("Should have 12 missed doses", 12, poorCompliance.recentMissedDoses)
        assertNotNull("Should have last missed dose", poorCompliance.lastMissedDose)
        
        // Test compliance classification
        assertTrue("Above 90% is excellent", classifyCompliance(95.0f) == "excellent")
        assertTrue("70-89% is good", classifyCompliance(80.0f) == "good")
        assertTrue("50-69% is fair", classifyCompliance(60.0f) == "fair")
        assertTrue("Below 50% is poor", classifyCompliance(40.0f) == "poor")
    }
    
    @Test
    fun testHealthMetricValidation() {
        // Test blood pressure validation
        assertTrue("120/80 is valid BP", isValidBloodPressure("120/80"))
        assertTrue("140/90 is valid BP", isValidBloodPressure("140/90"))
        assertFalse("300/200 is invalid BP", isValidBloodPressure("300/200"))
        assertFalse("abc/def is invalid BP", isValidBloodPressure("abc/def"))
        assertFalse("120 is invalid BP format", isValidBloodPressure("120"))
        
        // Test weight validation
        assertTrue("150.5 is valid weight", isValidWeight("150.5"))
        assertTrue("200 is valid weight", isValidWeight("200"))
        assertFalse("500 is invalid weight", isValidWeight("500"))
        assertFalse("abc is invalid weight", isValidWeight("abc"))
        assertFalse("Negative weight is invalid", isValidWeight("-10"))
        
        // Test temperature validation
        assertTrue("98.6 is valid temperature", isValidTemperature("98.6"))
        assertTrue("102.5 is valid temperature", isValidTemperature("102.5"))
        assertFalse("120 is invalid temperature", isValidTemperature("120"))
        assertFalse("80 is invalid temperature", isValidTemperature("80"))
    }
    
    @Test
    fun testGestationalAgeCalculations() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        
        // Test current gestational age
        val lmp = today.minus(kotlinx.datetime.DateTimeUnit.WEEK * 20) // 20 weeks ago
        val gestationalAge = calculateGestationalAge(lmp, today)
        assertEquals("Should be 20 weeks", 20, gestationalAge)
        
        // Test due date calculation
        val dueDate = calculateDueDate(lmp)
        val expectedDueDate = lmp.plus(kotlinx.datetime.DateTimeUnit.DAY * 280) // 40 weeks
        assertEquals("Due date should be 40 weeks from LMP", expectedDueDate, dueDate)
        
        // Test trimester classification
        assertEquals("Week 8 should be first trimester", 1, getTrimester(8))
        assertEquals("Week 20 should be second trimester", 2, getTrimester(20))
        assertEquals("Week 35 should be third trimester", 3, getTrimester(35))
    }
    
    @Test
    fun testVoiceCommandParsing() {
        // Test medication queries
        assertTrue("Should recognize medication query", 
            isMediacationQuery("When is my next medication?"))
        assertTrue("Should recognize dose query", 
            isMediacationQuery("What's my next dose?"))
        assertTrue("Should recognize pill query", 
            isMediacationQuery("Did I take my pills today?"))
        
        // Test health status queries
        assertTrue("Should recognize health query", 
            isHealthStatusQuery("How am I feeling today?"))
        assertTrue("Should recognize blood pressure query", 
            isHealthStatusQuery("What's my blood pressure?"))
        assertTrue("Should recognize baby status query", 
            isHealthStatusQuery("How is my baby doing?"))
        
        // Test emergency queries
        assertTrue("Should recognize emergency", 
            isEmergencyQuery("I need help, this is an emergency!"))
        assertTrue("Should recognize pain query", 
            isEmergencyQuery("I'm having severe chest pain"))
        assertTrue("Should recognize bleeding query", 
            isEmergencyQuery("I'm bleeding heavily"))
    }
    
    // Helper methods for testing
    
    private fun determineLabUrgency(results: Map<String, Double>): UrgencyLevel {
        val criticalThresholds = mapOf(
            "glucose_fasting" to Pair(70.0, 95.0),
            "blood_pressure_systolic" to Pair(90.0, 140.0),
            "platelets" to Pair(150.0, 450.0)
        )
        
        var hasCritical = false
        var hasElevated = false
        
        results.forEach { (test, value) ->
            criticalThresholds[test]?.let { (min, max) ->
                when {
                    value < min * 0.7 || value > max * 1.5 -> hasCritical = true
                    value < min || value > max -> hasElevated = true
                }
            }
        }
        
        return when {
            hasCritical -> UrgencyLevel.CRITICAL
            hasElevated -> UrgencyLevel.ELEVATED
            else -> UrgencyLevel.NORMAL
        }
    }
    
    private fun parseFrequencyToTimes(frequency: String): List<String> {
        val freqLower = frequency.lowercase()
        
        return when {
            freqLower.contains("once") || freqLower.contains("1") && freqLower.contains("day") -> 
                listOf("08:00")
            freqLower.contains("twice") || freqLower.contains("2") && freqLower.contains("day") -> 
                listOf("08:00", "20:00")
            freqLower.contains("three") || freqLower.contains("3") && freqLower.contains("day") -> 
                listOf("08:00", "14:00", "20:00")
            freqLower.contains("every 8 hours") -> 
                listOf("08:00", "16:00", "00:00")
            else -> listOf("08:00")
        }
    }
    
    private fun calculateEndDate(duration: String, startDate: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDate? {
        val durationLower = duration.lowercase()
        val numberRegex = Regex("""(\d+)""")
        val number = numberRegex.find(durationLower)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        
        return when {
            durationLower.contains("day") -> startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * number)
            durationLower.contains("week") -> startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * number * 7)
            durationLower.contains("month") -> startDate.plus(kotlinx.datetime.DateTimeUnit.MONTH * number)
            else -> startDate.plus(kotlinx.datetime.DateTimeUnit.DAY * 30)
        }
    }
    
    private fun classifyCompliance(percentage: Float): String {
        return when {
            percentage >= 90.0f -> "excellent"
            percentage >= 70.0f -> "good"
            percentage >= 50.0f -> "fair"
            else -> "poor"
        }
    }
    
    private fun isValidBloodPressure(value: String): Boolean {
        val bpRegex = Regex("""(\d{2,3})/(\d{2,3})""")
        val match = bpRegex.matchEntire(value) ?: return false
        
        val systolic = match.groupValues[1].toIntOrNull() ?: return false
        val diastolic = match.groupValues[2].toIntOrNull() ?: return false
        
        return systolic in 60..250 && diastolic in 40..150
    }
    
    private fun isValidWeight(value: String): Boolean {
        val weight = value.toDoubleOrNull() ?: return false
        return weight in 50.0..400.0
    }
    
    private fun isValidTemperature(value: String): Boolean {
        val temp = value.toDoubleOrNull() ?: return false
        return temp in 90.0..110.0 // Fahrenheit
    }
    
    private fun calculateGestationalAge(lmp: kotlinx.datetime.LocalDate, currentDate: kotlinx.datetime.LocalDate): Int {
        val daysDiff = currentDate.toEpochDays() - lmp.toEpochDays()
        return (daysDiff / 7).toInt()
    }
    
    private fun calculateDueDate(lmp: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDate {
        return lmp.plus(kotlinx.datetime.DateTimeUnit.DAY * 280)
    }
    
    private fun getTrimester(weeks: Int): Int {
        return when {
            weeks <= 13 -> 1
            weeks <= 27 -> 2
            else -> 3
        }
    }
    
    private fun isMediacationQuery(query: String): Boolean {
        val medicationPatterns = listOf(
            Regex("(?i).*(medication|medicine|pill|dose).*"),
            Regex("(?i).*(remind|reminder).*"),
            Regex("(?i).*(take|took|taking).*medication.*")
        )
        return medicationPatterns.any { it.matches(query) }
    }
    
    private fun isHealthStatusQuery(query: String): Boolean {
        val healthPatterns = listOf(
            Regex("(?i).*how.*feeling.*"),
            Regex("(?i).*health.*status.*"),
            Regex("(?i).*blood pressure.*"),
            Regex("(?i).*baby.*doing.*")
        )
        return healthPatterns.any { it.matches(query) }
    }
    
    private fun isEmergencyQuery(query: String): Boolean {
        val emergencyPatterns = listOf(
            Regex("(?i).*(emergency|urgent|help|crisis).*"),
            Regex("(?i).*(pain|bleeding|contractions).*"),
            Regex("(?i).*(cant.*breathe|chest.*pain).*")
        )
        return emergencyPatterns.any { it.matches(query) }
    }
}