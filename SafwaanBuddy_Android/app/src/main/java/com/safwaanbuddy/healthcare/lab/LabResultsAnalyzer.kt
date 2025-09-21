package com.safwaanbuddy.healthcare.lab

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.models.LabResult
import com.safwaanbuddy.healthcare.utils.NotificationHelper
import com.safwaanbuddy.healthcare.worker.EmergencyAlertWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lab Results Analysis Engine
 * Processes laboratory test results and provides emergency alerts for critical values
 */
@Singleton
class LabResultsAnalyzer @Inject constructor(
    private val context: Context,
    private val database: HealthcareDatabase,
    private val encryptionService: HealthcareEncryptionService,
    private val notificationHelper: NotificationHelper
) {
    
    companion object {
        private const val TAG = "LabResultsAnalyzer"
    }
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    // Pregnancy-specific normal ranges for common lab tests
    private val pregnancyNormalRanges = mapOf(
        // Blood Pressure (mmHg)
        "blood_pressure_systolic" to LabRange(90.0, 140.0, "mmHg", isCritical = true),
        "blood_pressure_diastolic" to LabRange(60.0, 90.0, "mmHg", isCritical = true),
        
        // Blood Tests
        "hemoglobin" to LabRange(11.0, 14.0, "g/dL", isCritical = false),
        "hematocrit" to LabRange(33.0, 42.0, "%", isCritical = false),
        "glucose_fasting" to LabRange(70.0, 95.0, "mg/dL", isCritical = true),
        "glucose_random" to LabRange(70.0, 140.0, "mg/dL", isCritical = true),
        "glucose_1hr_gtt" to LabRange(0.0, 140.0, "mg/dL", isCritical = true),
        "glucose_2hr_gtt" to LabRange(0.0, 153.0, "mg/dL", isCritical = true),
        "glucose_3hr_gtt" to LabRange(0.0, 140.0, "mg/dL", isCritical = true),
        
        // Kidney Function
        "creatinine" to LabRange(0.6, 1.1, "mg/dL", isCritical = true),
        "bun" to LabRange(7.0, 20.0, "mg/dL", isCritical = false),
        "protein_urine" to LabRange(0.0, 0.3, "g/24hr", isCritical = true),
        
        // Liver Function
        "alt" to LabRange(7.0, 56.0, "U/L", isCritical = false),
        "ast" to LabRange(10.0, 40.0, "U/L", isCritical = false),
        "bilirubin_total" to LabRange(0.3, 1.2, "mg/dL", isCritical = false),
        
        // Thyroid Function
        "tsh" to LabRange(0.1, 2.5, "mIU/L", isCritical = false), // First trimester range
        "free_t4" to LabRange(0.9, 1.7, "ng/dL", isCritical = false),
        
        // Infection Markers
        "white_blood_cells" to LabRange(4.0, 11.0, "K/uL", isCritical = true),
        "neutrophils" to LabRange(40.0, 70.0, "%", isCritical = false),
        
        // Anemia Screening
        "iron" to LabRange(60.0, 170.0, "mcg/dL", isCritical = false),
        "ferritin" to LabRange(15.0, 150.0, "ng/mL", isCritical = false),
        "folate" to LabRange(4.0, 20.0, "ng/mL", isCritical = false),
        "vitamin_b12" to LabRange(200.0, 900.0, "pg/mL", isCritical = false),
        
        // Platelet Function
        "platelets" to LabRange(150.0, 450.0, "K/uL", isCritical = true),
        
        // Cardiac Markers (if relevant)
        "troponin" to LabRange(0.0, 0.04, "ng/mL", isCritical = true)
    )
    
    /**
     * Process and analyze lab results from uploaded data
     */
    suspend fun processLabResults(
        patientId: Long,
        testType: String,
        rawResults: Map<String, Any>,
        testDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        providerName: String? = null,
        labFacility: String? = null
    ): LabAnalysisResult = withContext(Dispatchers.IO) {
        
        try {
            // Analyze the results
            val analysisResult = analyzeLabValues(rawResults)
            
            // Encrypt sensitive data
            val encryptedResults = encryptionService.encrypt(
                moshi.adapter(Map::class.java).toJson(rawResults),
                "lab_results_$patientId"
            )
            
            val encryptedFlaggedValues = if (analysisResult.flaggedValues.isNotEmpty()) {
                encryptionService.encrypt(
                    moshi.adapter(Map::class.java).toJson(analysisResult.flaggedValues),
                    "flagged_values_$patientId"
                )
            } else null
            
            val encryptedProviderName = providerName?.let {
                encryptionService.encrypt(it, "provider_name_$patientId")
            }
            
            val encryptedLabFacility = labFacility?.let {
                encryptionService.encrypt(it, "lab_facility_$patientId")
            }
            
            val encryptedNotes = analysisResult.clinicalNotes?.let {
                encryptionService.encrypt(it, "lab_notes_$patientId")
            }
            
            // Create lab result record
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val labResult = LabResult(
                patientId = patientId,
                testDate = testDate,
                testType = testType,
                resultsEncrypted = encryptedResults,
                flaggedValuesEncrypted = encryptedFlaggedValues,
                urgencyLevel = analysisResult.urgencyLevel.name.lowercase(),
                clinicianNotified = false,
                patientNotified = false,
                providerNameEncrypted = encryptedProviderName,
                labFacilityEncrypted = encryptedLabFacility,
                notesEncrypted = encryptedNotes,
                followUpRequired = analysisResult.urgencyLevel != UrgencyLevel.NORMAL,
                createdAt = now,
                updatedAt = now
            )
            
            // Save to database
            val resultId = database.labResultDao().insertLabResult(labResult)
            
            // Handle emergency alerts
            if (analysisResult.urgencyLevel == UrgencyLevel.CRITICAL) {
                triggerEmergencyAlert(patientId, analysisResult)
            }
            
            // Schedule follow-up notifications
            if (analysisResult.urgencyLevel == UrgencyLevel.ELEVATED) {
                scheduleFollowUpNotification(patientId, analysisResult)
            }
            
            Log.i(TAG, "Lab results processed for patient $patientId with urgency: ${analysisResult.urgencyLevel}")
            
            LabAnalysisResult(
                resultId = resultId,
                urgencyLevel = analysisResult.urgencyLevel,
                flaggedValues = analysisResult.flaggedValues,
                clinicalNotes = analysisResult.clinicalNotes,
                recommendedActions = analysisResult.recommendedActions,
                isSuccess = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process lab results", e)
            LabAnalysisResult(
                resultId = -1,
                urgencyLevel = UrgencyLevel.UNKNOWN,
                flaggedValues = emptyMap(),
                clinicalNotes = "Failed to process results: ${e.message}",
                recommendedActions = emptyList(),
                isSuccess = false,
                error = e.message
            )
        }
    }
    
    /**
     * Analyze lab values against normal ranges
     */
    private fun analyzeLabValues(results: Map<String, Any>): InternalAnalysisResult {
        val flaggedValues = mutableMapOf<String, FlaggedValue>()
        val criticalFlags = mutableListOf<String>()
        val elevatedFlags = mutableListOf<String>()
        val clinicalNotes = mutableListOf<String>()
        val recommendedActions = mutableListOf<String>()
        
        results.forEach { (testName, value) ->
            val normalRange = pregnancyNormalRanges[testName.lowercase()]
            val numericValue = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
            
            if (normalRange != null && numericValue != null) {
                when {
                    numericValue < normalRange.min -> {
                        val flaggedValue = FlaggedValue(
                            testName = testName,
                            value = numericValue,
                            normalRange = "${normalRange.min}-${normalRange.max} ${normalRange.unit}",
                            status = "Below Normal",
                            severity = if (normalRange.isCritical) "Critical" else "Low"
                        )
                        flaggedValues[testName] = flaggedValue
                        
                        if (normalRange.isCritical) {
                            criticalFlags.add(testName)
                        } else {
                            elevatedFlags.add(testName)
                        }
                    }
                    
                    numericValue > normalRange.max -> {
                        val flaggedValue = FlaggedValue(
                            testName = testName,
                            value = numericValue,
                            normalRange = "${normalRange.min}-${normalRange.max} ${normalRange.unit}",
                            status = "Above Normal",
                            severity = if (normalRange.isCritical) "Critical" else "High"
                        )
                        flaggedValues[testName] = flaggedValue
                        
                        if (normalRange.isCritical) {
                            criticalFlags.add(testName)
                        } else {
                            elevatedFlags.add(testName)
                        }
                    }
                }
            }
        }
        
        // Generate clinical notes and recommendations
        generateClinicalNotes(flaggedValues, clinicalNotes, recommendedActions)
        
        // Determine overall urgency level
        val urgencyLevel = when {
            criticalFlags.isNotEmpty() -> UrgencyLevel.CRITICAL
            elevatedFlags.isNotEmpty() -> UrgencyLevel.ELEVATED
            flaggedValues.isNotEmpty() -> UrgencyLevel.ELEVATED
            else -> UrgencyLevel.NORMAL
        }
        
        return InternalAnalysisResult(
            urgencyLevel = urgencyLevel,
            flaggedValues = flaggedValues,
            clinicalNotes = clinicalNotes.joinToString("\n"),
            recommendedActions = recommendedActions
        )
    }
    
    /**
     * Generate clinical notes and recommendations based on flagged values
     */
    private fun generateClinicalNotes(
        flaggedValues: Map<String, FlaggedValue>,
        clinicalNotes: MutableList<String>,
        recommendedActions: MutableList<String>
    ) {
        flaggedValues.forEach { (testName, flaggedValue) ->
            when (testName.lowercase()) {
                "blood_pressure_systolic", "blood_pressure_diastolic" -> {
                    if (flaggedValue.severity == "Critical") {
                        clinicalNotes.add("Blood pressure readings indicate potential preeclampsia risk")
                        recommendedActions.add("Immediate medical evaluation required")
                        recommendedActions.add("Monitor for symptoms: headache, vision changes, upper abdominal pain")
                    }
                }
                
                "glucose_fasting", "glucose_random", "glucose_1hr_gtt", "glucose_2hr_gtt", "glucose_3hr_gtt" -> {
                    if (flaggedValue.value > (pregnancyNormalRanges[testName]?.max ?: 140.0)) {
                        clinicalNotes.add("Elevated glucose levels suggest gestational diabetes risk")
                        recommendedActions.add("Dietary consultation recommended")
                        recommendedActions.add("Blood glucose monitoring")
                        recommendedActions.add("Follow-up with endocrinologist if indicated")
                    }
                }
                
                "protein_urine" -> {
                    if (flaggedValue.value > 0.3) {
                        clinicalNotes.add("Proteinuria detected - may indicate preeclampsia or kidney issues")
                        recommendedActions.add("Repeat urine analysis")
                        recommendedActions.add("Monitor blood pressure closely")
                    }
                }
                
                "hemoglobin", "hematocrit" -> {
                    if (flaggedValue.status == "Below Normal") {
                        clinicalNotes.add("Iron deficiency anemia common in pregnancy")
                        recommendedActions.add("Iron supplementation")
                        recommendedActions.add("Dietary counseling for iron-rich foods")
                    }
                }
                
                "platelets" -> {
                    if (flaggedValue.value < 150.0) {
                        clinicalNotes.add("Thrombocytopenia - monitor for bleeding risk")
                        recommendedActions.add("Avoid aspirin and NSAIDs")
                        recommendedActions.add("Monitor platelet count")
                    }
                }
                
                "white_blood_cells" -> {
                    if (flaggedValue.value > 11.0) {
                        clinicalNotes.add("Elevated WBC may indicate infection")
                        recommendedActions.add("Evaluate for signs of infection")
                        recommendedActions.add("Consider antibiotic therapy if indicated")
                    }
                }
                
                "creatinine" -> {
                    if (flaggedValue.value > 1.1) {
                        clinicalNotes.add("Elevated creatinine suggests kidney function impairment")
                        recommendedActions.add("Nephrology consultation")
                        recommendedActions.add("Monitor fluid balance")
                    }
                }
                
                "tsh" -> {
                    when {
                        flaggedValue.value > 2.5 -> {
                            clinicalNotes.add("Elevated TSH suggests hypothyroidism")
                            recommendedActions.add("Endocrinology consultation")
                            recommendedActions.add("Consider levothyroxine therapy")
                        }
                        flaggedValue.value < 0.1 -> {
                            clinicalNotes.add("Suppressed TSH suggests hyperthyroidism")
                            recommendedActions.add("Endocrinology consultation")
                            recommendedActions.add("Monitor for maternal and fetal complications")
                        }
                    }
                }
            }
        }
        
        // General pregnancy recommendations
        if (flaggedValues.isNotEmpty()) {
            recommendedActions.add("Discuss results with healthcare provider")
            recommendedActions.add("Continue prenatal vitamins")
            recommendedActions.add("Monitor fetal movement and well-being")
        }
    }
    
    /**
     * Trigger emergency alert for critical lab values
     */
    private suspend fun triggerEmergencyAlert(patientId: Long, analysisResult: InternalAnalysisResult) {
        val criticalValues = analysisResult.flaggedValues.filter { it.value.severity == "Critical" }
        
        if (criticalValues.isNotEmpty()) {
            val alertMessage = buildString {
                append("CRITICAL LAB RESULTS DETECTED:\n")
                criticalValues.forEach { (testName, flaggedValue) ->
                    append("• $testName: ${flaggedValue.value} (Normal: ${flaggedValue.normalRange})\n")
                }
                append("\nIMMEDIATE MEDICAL ATTENTION REQUIRED")
            }
            
            // Send immediate notification
            notificationHelper.showEmergencyAlert(
                patientId.toInt(),
                "Critical Lab Results",
                alertMessage
            )
            
            // Schedule emergency alert worker
            val emergencyWork = OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
                .setInputData(workDataOf(
                    "patient_id" to patientId,
                    "alert_type" to "critical_lab_result",
                    "alert_message" to alertMessage
                ))
                .addTag("emergency_alert")
                .build()
            
            WorkManager.getInstance(context).enqueue(emergencyWork)
            
            // Mark as requiring clinician notification
            database.labResultDao().markClinicianNotified(
                analysisResult.resultId,
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
        }
    }
    
    /**
     * Schedule follow-up notification for elevated values
     */
    private fun scheduleFollowUpNotification(patientId: Long, analysisResult: InternalAnalysisResult) {
        val message = buildString {
            append("Lab results show elevated values that require follow-up:\n")
            analysisResult.flaggedValues.forEach { (testName, flaggedValue) ->
                append("• $testName: ${flaggedValue.status}\n")
            }
            append("\nPlease schedule follow-up with your healthcare provider.")
        }
        
        notificationHelper.showSafetyAlert(
            patientId.toInt(),
            "Lab Results Follow-up Required",
            message
        )
    }
    
    /**
     * Get trend analysis for a specific lab test
     */
    suspend fun getLabTrendAnalysis(
        patientId: Long,
        testType: String,
        months: Int = 6
    ): LabTrendAnalysis = withContext(Dispatchers.IO) {
        val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startDate = endDate.minus(months, DateTimeUnit.MONTH)
        
        val results = database.labResultDao().getLabResultsByDateRange(patientId, startDate, endDate)
        
        // This would require more complex analysis of trends
        // For now, return basic trend information
        LabTrendAnalysis(
            testType = testType,
            totalTests = 0, // Would count from results
            trendDirection = TrendDirection.STABLE,
            lastValue = null,
            averageValue = null,
            recommendation = "Continue monitoring as scheduled"
        )
    }
}

/**
 * Data classes for lab analysis
 */
data class LabRange(
    val min: Double,
    val max: Double,
    val unit: String,
    val isCritical: Boolean = false
)

data class FlaggedValue(
    val testName: String,
    val value: Double,
    val normalRange: String,
    val status: String, // "Above Normal", "Below Normal"
    val severity: String // "Critical", "High", "Low"
)

data class LabAnalysisResult(
    val resultId: Long,
    val urgencyLevel: UrgencyLevel,
    val flaggedValues: Map<String, FlaggedValue>,
    val clinicalNotes: String?,
    val recommendedActions: List<String>,
    val isSuccess: Boolean,
    val error: String? = null
)

private data class InternalAnalysisResult(
    val urgencyLevel: UrgencyLevel,
    val flaggedValues: Map<String, FlaggedValue>,
    val clinicalNotes: String?,
    val recommendedActions: List<String>,
    val resultId: Long = -1
)

data class LabTrendAnalysis(
    val testType: String,
    val totalTests: Int,
    val trendDirection: TrendDirection,
    val lastValue: Double?,
    val averageValue: Double?,
    val recommendation: String
)

enum class UrgencyLevel {
    NORMAL,
    ELEVATED,
    CRITICAL,
    UNKNOWN
}

enum class TrendDirection {
    IMPROVING,
    STABLE,
    WORSENING,
    INSUFFICIENT_DATA
}