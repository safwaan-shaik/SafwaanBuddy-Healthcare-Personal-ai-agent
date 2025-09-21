package com.safwaanbuddy.healthcare.compliance

import android.content.Context
import android.util.Log
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.models.VoiceCommandLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HIPAA Compliance Manager
 * Handles healthcare privacy compliance, audit logging, and data governance
 */
@Singleton
class HIPAAComplianceManager @Inject constructor(
    private val context: Context,
    private val database: HealthcareDatabase,
    private val encryptionService: HealthcareEncryptionService
) {
    
    companion object {
        private const val TAG = "HIPAACompliance"
        private const val AUDIT_LOG_CATEGORY = "HIPAA_AUDIT"
    }
    
    /**
     * Log data access for HIPAA compliance
     */
    suspend fun logDataAccess(
        userId: String,
        patientId: Long,
        dataType: String,
        action: String,
        accessReason: String? = null,
        ipAddress: String? = null,
        deviceInfo: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            
            val auditEntry = AuditLogEntry(
                userId = userId,
                patientId = patientId,
                dataType = dataType,
                action = action,
                accessReason = accessReason,
                ipAddress = ipAddress,
                deviceInfo = deviceInfo,
                timestamp = timestamp,
                complianceLevel = determineComplianceLevel(dataType, action)
            )
            
            // Log to voice command table as it supports audit logging
            val voiceLog = VoiceCommandLog(
                patientId = patientId,
                commandText = "AUDIT_LOG: $action on $dataType",
                intentClassification = AUDIT_LOG_CATEGORY,
                responseGenerated = buildAuditLogJson(auditEntry),
                confidenceScore = 1.0f,
                processingTimeMs = 0L,
                wasSuccessful = true,
                errorMessage = null,
                timestamp = timestamp
            )
            
            database.voiceCommandLogDao().insertVoiceCommand(voiceLog)
            
            Log.d(TAG, "HIPAA audit log recorded: $action on $dataType for patient $patientId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log HIPAA audit entry", e)
            // CRITICAL: Audit logging failure should be escalated
            reportAuditFailure(e)
        }
    }
    
    /**
     * Log authentication events
     */
    suspend fun logAuthenticationEvent(
        userId: String,
        eventType: String,
        success: Boolean,
        ipAddress: String? = null,
        deviceInfo: String? = null,
        failureReason: String? = null
    ) {
        logDataAccess(
            userId = userId,
            patientId = 0L, // System-level event
            dataType = "AUTHENTICATION",
            action = eventType,
            accessReason = if (success) "Authentication successful" else "Authentication failed: $failureReason",
            ipAddress = ipAddress,
            deviceInfo = deviceInfo
        )
    }
    
    /**
     * Log prescription access
     */
    suspend fun logPrescriptionAccess(
        userId: String,
        patientId: Long,
        prescriptionId: Long,
        action: String,
        accessReason: String = "Patient care"
    ) {
        logDataAccess(
            userId = userId,
            patientId = patientId,
            dataType = "PRESCRIPTION",
            action = "$action (ID: $prescriptionId)",
            accessReason = accessReason,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Log lab results access
     */
    suspend fun logLabResultsAccess(
        userId: String,
        patientId: Long,
        resultId: Long,
        action: String,
        accessReason: String = "Patient care"
    ) {
        logDataAccess(
            userId = userId,
            patientId = patientId,
            dataType = "LAB_RESULTS",
            action = "$action (ID: $resultId)",
            accessReason = accessReason,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Log medication access
     */
    suspend fun logMedicationAccess(
        userId: String,
        patientId: Long,
        medicationId: Long,
        action: String,
        accessReason: String = "Medication management"
    ) {
        logDataAccess(
            userId = userId,
            patientId = patientId,
            dataType = "MEDICATION",
            action = "$action (ID: $medicationId)",
            accessReason = accessReason,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Log patient data access
     */
    suspend fun logPatientDataAccess(
        userId: String,
        patientId: Long,
        action: String,
        accessReason: String = "Patient care"
    ) {
        logDataAccess(
            userId = userId,
            patientId = patientId,
            dataType = "PATIENT_DATA",
            action = action,
            accessReason = accessReason,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Log data export events
     */
    suspend fun logDataExport(
        userId: String,
        patientId: Long,
        exportType: String,
        destinationType: String,
        accessReason: String
    ) {
        logDataAccess(
            userId = userId,
            patientId = patientId,
            dataType = "DATA_EXPORT",
            action = "EXPORT_$exportType to $destinationType",
            accessReason = accessReason,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Log data breach detection
     */
    suspend fun logSecurityBreach(
        breachType: String,
        affectedPatients: List<Long>,
        severity: String,
        description: String,
        mitigationActions: String
    ) {
        affectedPatients.forEach { patientId ->
            logDataAccess(
                userId = "SYSTEM",
                patientId = patientId,
                dataType = "SECURITY_BREACH",
                action = "BREACH_DETECTED: $breachType ($severity)",
                accessReason = "Security incident: $description. Mitigation: $mitigationActions",
                deviceInfo = getDeviceInfo()
            )
        }
        
        // Additional breach reporting would go here
        Log.w(TAG, "SECURITY BREACH DETECTED: $breachType affecting ${affectedPatients.size} patients")
    }
    
    /**
     * Generate compliance report
     */
    suspend fun generateComplianceReport(
        startDate: kotlinx.datetime.LocalDateTime,
        endDate: kotlinx.datetime.LocalDateTime,
        patientId: Long? = null
    ): ComplianceReport = withContext(Dispatchers.IO) {
        try {
            // Get all audit logs within date range
            val auditLogs = if (patientId != null) {
                database.voiceCommandLogDao().getRecentCommandsByPatient(patientId, 1000)
            } else {
                database.voiceCommandLogDao().getCommandsByIntent(AUDIT_LOG_CATEGORY)
            }.first().filter { 
                it.timestamp >= startDate && it.timestamp <= endDate 
            }
            
            val accessCounts = mutableMapOf<String, Int>()
            val userAccess = mutableMapOf<String, Int>()
            val dataTypes = mutableMapOf<String, Int>()
            var securityIncidents = 0
            
            auditLogs.forEach { log ->
                // Parse audit data from the log
                if (log.intentClassification == AUDIT_LOG_CATEGORY) {
                    val action = extractActionFromCommand(log.commandText)
                    val dataType = extractDataTypeFromCommand(log.commandText)
                    
                    accessCounts[action] = accessCounts.getOrDefault(action, 0) + 1
                    dataTypes[dataType] = dataTypes.getOrDefault(dataType, 0) + 1
                    
                    if (log.commandText.contains("BREACH")) {
                        securityIncidents++
                    }
                }
            }
            
            ComplianceReport(
                reportPeriod = "$startDate to $endDate",
                totalAuditEntries = auditLogs.size,
                accessByAction = accessCounts,
                accessByDataType = dataTypes,
                securityIncidents = securityIncidents,
                complianceScore = calculateComplianceScore(auditLogs),
                recommendations = generateComplianceRecommendations(auditLogs, securityIncidents)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate compliance report", e)
            ComplianceReport(
                reportPeriod = "$startDate to $endDate",
                totalAuditEntries = 0,
                accessByAction = emptyMap(),
                accessByDataType = emptyMap(),
                securityIncidents = 0,
                complianceScore = 0.0f,
                recommendations = listOf("Failed to generate report: ${e.message}")
            )
        }
    }
    
    /**
     * Validate data access permissions
     */
    suspend fun validateDataAccess(
        userId: String,
        patientId: Long,
        dataType: String,
        requestedAction: String
    ): AccessValidationResult = withContext(Dispatchers.IO) {
        try {
            // In a real implementation, this would check against:
            // - User roles and permissions
            // - Patient consent records
            // - Minimum necessary standards
            // - Break-glass access controls
            
            val hasPermission = when (dataType) {
                "PATIENT_DATA", "PRESCRIPTION", "MEDICATION" -> {
                    // Check if user has patient care permissions
                    checkPatientCareAccess(userId, patientId)
                }
                "LAB_RESULTS" -> {
                    // Check if user has lab results access
                    checkLabResultsAccess(userId, patientId)
                }
                "AUTHENTICATION" -> {
                    // System-level access
                    true
                }
                else -> false
            }
            
            if (hasPermission) {
                // Log successful access validation
                logDataAccess(
                    userId = userId,
                    patientId = patientId,
                    dataType = dataType,
                    action = "ACCESS_GRANTED: $requestedAction",
                    accessReason = "Permission validation successful"
                )
                
                AccessValidationResult(
                    isAllowed = true,
                    reason = "Access granted",
                    requiredActions = emptyList()
                )
            } else {
                // Log access denial
                logDataAccess(
                    userId = userId,
                    patientId = patientId,
                    dataType = dataType,
                    action = "ACCESS_DENIED: $requestedAction",
                    accessReason = "Insufficient permissions"
                )
                
                AccessValidationResult(
                    isAllowed = false,
                    reason = "Insufficient permissions for $dataType access",
                    requiredActions = listOf("Contact administrator for access")
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Access validation failed", e)
            AccessValidationResult(
                isAllowed = false,
                reason = "Access validation error: ${e.message}",
                requiredActions = listOf("Contact system administrator")
            )
        }
    }
    
    /**
     * Check minimum necessary access
     */
    suspend fun checkMinimumNecessary(
        userId: String,
        requestedData: List<String>,
        purpose: String
    ): MinimumNecessaryResult {
        // Analyze if the requested data is the minimum necessary for the stated purpose
        val necessaryData = determineMinimumNecessaryData(purpose)
        val excessiveData = requestedData.filter { it !in necessaryData }
        
        return MinimumNecessaryResult(
            isCompliant = excessiveData.isEmpty(),
            necessaryData = necessaryData,
            excessiveData = excessiveData,
            recommendations = if (excessiveData.isNotEmpty()) {
                listOf("Remove access to: ${excessiveData.joinToString(", ")}")
            } else {
                listOf("Data access complies with minimum necessary standard")
            }
        )
    }
    
    /**
     * Handle patient consent management
     */
    suspend fun recordPatientConsent(
        patientId: Long,
        consentType: String,
        consentGiven: Boolean,
        consentDetails: String,
        witnessInfo: String? = null
    ) {
        logDataAccess(
            userId = "PATIENT",
            patientId = patientId,
            dataType = "CONSENT",
            action = if (consentGiven) "CONSENT_GRANTED" else "CONSENT_WITHDRAWN",
            accessReason = "$consentType: $consentDetails. Witness: ${witnessInfo ?: "Self-attested"}"
        )
    }
    
    /**
     * Emergency access logging (break-glass)
     */
    suspend fun logEmergencyAccess(
        userId: String,
        patientId: Long,
        emergencyType: String,
        justification: String,
        dataAccessed: List<String>
    ) {
        dataAccessed.forEach { dataType ->
            logDataAccess(
                userId = userId,
                patientId = patientId,
                dataType = dataType,
                action = "EMERGENCY_ACCESS",
                accessReason = "Emergency: $emergencyType. Justification: $justification"
            )
        }
        
        Log.w(TAG, "Emergency access logged for patient $patientId by user $userId")
    }
    
    // Helper methods
    
    private fun determineComplianceLevel(dataType: String, action: String): String {
        return when {
            dataType.contains("BREACH") -> "CRITICAL"
            action.contains("EMERGENCY") -> "HIGH"
            action.contains("EXPORT") -> "HIGH"
            action.contains("DELETE") -> "MEDIUM"
            else -> "NORMAL"
        }
    }
    
    private fun buildAuditLogJson(auditEntry: AuditLogEntry): String {
        return """
            {
                "userId": "${auditEntry.userId}",
                "patientId": ${auditEntry.patientId},
                "dataType": "${auditEntry.dataType}",
                "action": "${auditEntry.action}",
                "accessReason": "${auditEntry.accessReason}",
                "ipAddress": "${auditEntry.ipAddress}",
                "deviceInfo": "${auditEntry.deviceInfo}",
                "timestamp": "${auditEntry.timestamp}",
                "complianceLevel": "${auditEntry.complianceLevel}"
            }
        """.trimIndent()
    }
    
    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }
    
    private fun reportAuditFailure(exception: Exception) {
        // In production, this would trigger immediate alerts to compliance team
        Log.e(TAG, "CRITICAL: Audit logging failure", exception)
    }
    
    private fun extractActionFromCommand(command: String): String {
        return command.substringAfter("AUDIT_LOG: ").substringBefore(" on ")
    }
    
    private fun extractDataTypeFromCommand(command: String): String {
        return command.substringAfter(" on ").substringBefore(" (")
    }
    
    private fun calculateComplianceScore(auditLogs: List<VoiceCommandLog>): Float {
        if (auditLogs.isEmpty()) return 100.0f
        
        val totalLogs = auditLogs.size
        val securityIncidents = auditLogs.count { it.commandText.contains("BREACH") }
        val accessDenials = auditLogs.count { it.commandText.contains("DENIED") }
        
        val penalty = (securityIncidents * 20) + (accessDenials * 5)
        return maxOf(0.0f, 100.0f - (penalty.toFloat() / totalLogs * 100))
    }
    
    private fun generateComplianceRecommendations(auditLogs: List<VoiceCommandLog>, securityIncidents: Int): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (securityIncidents > 0) {
            recommendations.add("Review and address $securityIncidents security incidents")
        }
        
        val accessDenials = auditLogs.count { it.commandText.contains("DENIED") }
        if (accessDenials > auditLogs.size * 0.1) {
            recommendations.add("High number of access denials - review user permissions")
        }
        
        if (auditLogs.isEmpty()) {
            recommendations.add("No audit logs found - verify logging system is functioning")
        }
        
        recommendations.add("Conduct regular access reviews")
        recommendations.add("Ensure all staff complete HIPAA training")
        recommendations.add("Implement regular vulnerability assessments")
        
        return recommendations
    }
    
    private suspend fun checkPatientCareAccess(userId: String, patientId: Long): Boolean {
        // In a real implementation, this would check user roles and assignments
        return true // Simplified for demonstration
    }
    
    private suspend fun checkLabResultsAccess(userId: String, patientId: Long): Boolean {
        // In a real implementation, this would check specific lab access permissions
        return true // Simplified for demonstration
    }
    
    private fun determineMinimumNecessaryData(purpose: String): List<String> {
        return when (purpose.lowercase()) {
            "medication management" -> listOf("PATIENT_DATA", "PRESCRIPTION", "MEDICATION")
            "lab review" -> listOf("PATIENT_DATA", "LAB_RESULTS")
            "emergency care" -> listOf("PATIENT_DATA", "PRESCRIPTION", "LAB_RESULTS", "MEDICATION")
            "billing" -> listOf("PATIENT_DATA")
            else -> listOf("PATIENT_DATA")
        }
    }
}

/**
 * Data classes for HIPAA compliance
 */
data class AuditLogEntry(
    val userId: String,
    val patientId: Long,
    val dataType: String,
    val action: String,
    val accessReason: String?,
    val ipAddress: String?,
    val deviceInfo: String?,
    val timestamp: kotlinx.datetime.LocalDateTime,
    val complianceLevel: String
)

data class ComplianceReport(
    val reportPeriod: String,
    val totalAuditEntries: Int,
    val accessByAction: Map<String, Int>,
    val accessByDataType: Map<String, Int>,
    val securityIncidents: Int,
    val complianceScore: Float,
    val recommendations: List<String>
)

data class AccessValidationResult(
    val isAllowed: Boolean,
    val reason: String,
    val requiredActions: List<String>
)

data class MinimumNecessaryResult(
    val isCompliant: Boolean,
    val necessaryData: List<String>,
    val excessiveData: List<String>,
    val recommendations: List<String>
)