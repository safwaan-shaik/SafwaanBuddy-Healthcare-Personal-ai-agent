package com.safwaanbuddy.healthcare.prescription

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import com.safwaanbuddy.healthcare.data.models.Prescription
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Prescription OCR Processing Service
 * Advanced prescription parsing using ML Kit for medication extraction
 */
@Singleton
class PrescriptionOCRService @Inject constructor(
    private val context: Context,
    private val encryptionService: HealthcareEncryptionService,
    private val database: HealthcareDatabase
) {
    
    companion object {
        private const val TAG = "PrescriptionOCR"
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    // Pregnancy medication safety database
    private val pregnancyCategories = mapOf(
        "safe" to listOf(
            "prenatal vitamin", "folic acid", "iron", "calcium", "vitamin d",
            "acetaminophen", "paracetamol", "methyldopa", "labetalol", "insulin"
        ),
        "caution" to listOf(
            "ibuprofen", "aspirin", "naproxen", "codeine", "hydrocodone",
            "prednisone", "metformin", "diphenhydramine"
        ),
        "avoid" to listOf(
            "warfarin", "ace inhibitors", "angiotensin", "isotretinoin",
            "methotrexate", "lithium", "phenytoin", "valproic acid", "atenolol"
        )
    )
    
    /**
     * Process prescription image and extract medication information
     */
    suspend fun processPrescriptionImage(
        imageUri: Uri,
        patientId: Long
    ): PrescriptionProcessingResult {
        return try {
            // Extract text from image using ML Kit
            val extractedText = extractTextFromImage(imageUri)
            
            if (extractedText.isBlank()) {
                return PrescriptionProcessingResult.Error("No text could be extracted from the image")
            }
            
            // Parse medications from extracted text
            val parsedMedications = parseMedicationsFromText(extractedText)
            
            if (parsedMedications.isEmpty()) {
                return PrescriptionProcessingResult.Error("No medications found in the prescription")
            }
            
            // Add pregnancy safety analysis
            val enhancedMedications = addPregnancySafetyAnalysis(parsedMedications)
            
            // Encrypt sensitive data
            val encryptedImagePath = encryptionService.encrypt(imageUri.toString(), "prescription_image_$patientId")
            val encryptedOcrText = encryptionService.encrypt(extractedText, "prescription_ocr_$patientId")
            val encryptedMedications = encryptionService.encrypt(
                moshi.adapter(PrescriptionData::class.java).toJson(
                    PrescriptionData(
                        medications = enhancedMedications,
                        extractedText = extractedText,
                        processingTimestamp = Clock.System.now().toString()
                    )
                ),
                "prescription_data_$patientId"
            )
            
            // Create prescription record
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val prescription = Prescription(
                patientId = patientId,
                imagePathEncrypted = encryptedImagePath,
                ocrTextEncrypted = encryptedOcrText,
                parsedMedicationsEncrypted = encryptedMedications,
                verificationStatus = "pending",
                clinicianVerified = false,
                prescriberNameEncrypted = extractPrescriberName(extractedText)?.let { 
                    encryptionService.encrypt(it, "prescriber_name_$patientId") 
                },
                prescriptionDate = extractPrescriptionDate(extractedText),
                createdAt = now,
                updatedAt = now
            )
            
            // Save to database
            val prescriptionId = database.prescriptionDao().insertPrescription(prescription)
            
            PrescriptionProcessingResult.Success(
                prescriptionId = prescriptionId,
                medications = enhancedMedications,
                extractedText = extractedText,
                safetyAlerts = generateSafetyAlerts(enhancedMedications)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing prescription image", e)
            PrescriptionProcessingResult.Error("Failed to process prescription: ${e.message}")
        }
    }
    
    /**
     * Extract text from image using ML Kit Text Recognition
     */
    private suspend fun extractTextFromImage(imageUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, imageUri)
                
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        Log.d(TAG, "Extracted text: $extractedText")
                        continuation.resume(extractedText)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Text recognition failed", exception)
                        continuation.resumeWithException(exception)
                    }
                    
            } catch (e: IOException) {
                Log.e(TAG, "Failed to create InputImage", e)
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Parse medications from extracted OCR text
     */
    private fun parseMedicationsFromText(text: String): List<MedicationInfo> {
        val medications = mutableListOf<MedicationInfo>()
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotBlank() }
        
        var currentMedication: MedicationInfo? = null
        
        for (line in lines) {
            // Skip common non-medication lines
            if (isNonMedicationLine(line)) continue
            
            // Check if line starts a new medication
            val medicationName = extractMedicationName(line)
            if (medicationName != null) {
                // Save previous medication if exists
                currentMedication?.let { medications.add(it) }
                
                // Start new medication
                currentMedication = MedicationInfo(
                    name = medicationName,
                    strength = extractStrength(line),
                    form = extractForm(line),
                    frequency = extractFrequency(line),
                    duration = extractDuration(line),
                    instructions = extractInstructions(line),
                    quantity = extractQuantity(line)
                )
            } else {
                // Add information to current medication
                currentMedication?.let { med ->
                    if (med.strength.isNullOrBlank()) {
                        med.strength = extractStrength(line)
                    }
                    if (med.frequency.isNullOrBlank()) {
                        med.frequency = extractFrequency(line)
                    }
                    if (med.duration.isNullOrBlank()) {
                        med.duration = extractDuration(line)
                    }
                    if (med.instructions.isNullOrBlank()) {
                        med.instructions = extractInstructions(line)
                    }
                    if (med.quantity.isNullOrBlank()) {
                        med.quantity = extractQuantity(line)
                    }
                }
            }
        }
        
        // Add last medication
        currentMedication?.let { medications.add(it) }
        
        // Filter and validate medications
        return medications.filter { it.name.isNotBlank() && it.name.length > 2 }
    }
    
    /**
     * Check if line contains non-medication information
     */
    private fun isNonMedicationLine(line: String): Boolean {
        val lowerLine = line.lowercase()
        val skipWords = listOf(
            "dr.", "doctor", "clinic", "hospital", "patient", "date", "dob",
            "address", "phone", "signature", "license", "dea", "npi"
        )
        return skipWords.any { lowerLine.contains(it) }
    }
    
    /**
     * Extract medication name from text line
     */
    private fun extractMedicationName(line: String): String? {
        // Look for medication name patterns
        val patterns = listOf(
            Regex("""^([A-Z][a-zA-Z]+(?:\s+[A-Z][a-zA-Z]+)*)"""), // Capitalized words
            Regex("""([A-Z][a-z]+)\s*(?:\d+(?:mg|g|ml|mcg))?""") // Name followed by dosage
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.length > 2 && !isCommonNonMedicationWord(name)) {
                    return name
                }
            }
        }
        return null
    }
    
    /**
     * Check if word is commonly found in prescriptions but not a medication
     */
    private fun isCommonNonMedicationWord(word: String): Boolean {
        val nonMedicationWords = listOf(
            "Take", "Tablet", "Capsule", "Daily", "Times", "Refill", "Generic",
            "Brand", "Prescription", "Instructions", "Directions"
        )
        return nonMedicationWords.any { it.equals(word, ignoreCase = true) }
    }
    
    /**
     * Extract medication strength/dosage
     */
    private fun extractStrength(line: String): String? {
        val patterns = listOf(
            Regex("""(\d+(?:\.\d+)?\s*(?:mg|g|ml|mcg|units?|iu))""", RegexOption.IGNORE_CASE),
            Regex("""(\d+(?:\.\d+)?)\s*(milligrams?|grams?|milliliters?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.value.trim()
            }
        }
        return null
    }
    
    /**
     * Extract medication form (tablet, capsule, etc.)
     */
    private fun extractForm(line: String): String? {
        val forms = listOf("tablet", "capsule", "liquid", "injection", "cream", "drops", "patch", "inhaler")
        val lowerLine = line.lowercase()
        
        for (form in forms) {
            if (lowerLine.contains(form)) {
                return form.replaceFirstChar { it.uppercase() }
            }
        }
        return "Tablet" // Default
    }
    
    /**
     * Extract frequency information
     */
    private fun extractFrequency(line: String): String? {
        val patterns = listOf(
            Regex("""(once|twice|thrice|\d+\s*times?)\s*(daily|per day|a day)""", RegexOption.IGNORE_CASE),
            Regex("""every\s*(\d+)\s*hours?""", RegexOption.IGNORE_CASE),
            Regex("""(morning|evening|night|bedtime)""", RegexOption.IGNORE_CASE),
            Regex("""(bid|tid|qid|q\d+h)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.value.trim()
            }
        }
        return null
    }
    
    /**
     * Extract duration information
     */
    private fun extractDuration(line: String): String? {
        val pattern = Regex("""for\s*(\d+)\s*(days?|weeks?|months?)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        return match?.value?.trim()
    }
    
    /**
     * Extract special instructions
     */
    private fun extractInstructions(line: String): String? {
        val instructionPatterns = listOf(
            Regex("""take with (food|water|meals)""", RegexOption.IGNORE_CASE),
            Regex("""(before|after) (meals|food|bedtime)""", RegexOption.IGNORE_CASE),
            Regex("""on empty stomach""", RegexOption.IGNORE_CASE),
            Regex("""do not (crush|chew|break)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in instructionPatterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.value.trim()
            }
        }
        return null
    }
    
    /**
     * Extract quantity prescribed
     */
    private fun extractQuantity(line: String): String? {
        val patterns = listOf(
            Regex("""(?:#|qty|quantity)\s*(\d+)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(?:tablets?|capsules?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Extract prescriber name
     */
    private fun extractPrescriberName(text: String): String? {
        val patterns = listOf(
            Regex("""Dr\.?\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)"""),
            Regex("""([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*),?\s*M\.?D\.?""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return "Dr. ${match.groupValues[1]}"
            }
        }
        return null
    }
    
    /**
     * Extract prescription date
     */
    private fun extractPrescriptionDate(text: String): kotlinx.datetime.LocalDate? {
        val patterns = listOf(
            Regex("""(\d{1,2}/\d{1,2}/\d{4})"""),
            Regex("""(\d{1,2}-\d{1,2}-\d{4})"""),
            Regex("""(\d{4}-\d{1,2}-\d{1,2})""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    val dateString = match.groupValues[1]
                    // Parse different date formats
                    return when {
                        dateString.contains("/") -> {
                            val parts = dateString.split("/")
                            kotlinx.datetime.LocalDate(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                        }
                        dateString.contains("-") && dateString.startsWith("20") -> {
                            kotlinx.datetime.LocalDate.parse(dateString)
                        }
                        else -> {
                            val parts = dateString.split("-")
                            kotlinx.datetime.LocalDate(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date: ${match.groupValues[1]}", e)
                }
            }
        }
        return null
    }
    
    /**
     * Add pregnancy safety analysis to medications
     */
    private fun addPregnancySafetyAnalysis(medications: List<MedicationInfo>): List<MedicationInfo> {
        return medications.map { medication ->
            val medNameLower = medication.name.lowercase()
            val safetyCategory = pregnancyCategories.entries.find { (_, medList) ->
                medList.any { safeMed -> medNameLower.contains(safeMed) }
            }?.key ?: "unknown"
            
            medication.copy(pregnancySafetyCategory = safetyCategory)
        }
    }
    
    /**
     * Generate safety alerts for medications
     */
    private fun generateSafetyAlerts(medications: List<MedicationInfo>): List<SafetyAlert> {
        val alerts = mutableListOf<SafetyAlert>()
        
        medications.forEach { medication ->
            when (medication.pregnancySafetyCategory) {
                "avoid" -> alerts.add(
                    SafetyAlert(
                        medicationName = medication.name,
                        level = SafetyAlertLevel.HIGH,
                        message = "${medication.name} may not be safe during pregnancy. Consult your doctor immediately."
                    )
                )
                "caution" -> alerts.add(
                    SafetyAlert(
                        medicationName = medication.name,
                        level = SafetyAlertLevel.MEDIUM,
                        message = "${medication.name} requires caution during pregnancy. Discuss with your healthcare provider."
                    )
                )
                "unknown" -> alerts.add(
                    SafetyAlert(
                        medicationName = medication.name,
                        level = SafetyAlertLevel.LOW,
                        message = "Pregnancy safety information for ${medication.name} is not available. Consult your healthcare provider."
                    )
                )
            }
        }
        
        return alerts
    }
}

/**
 * Data classes for prescription processing
 */
data class MedicationInfo(
    val name: String,
    var strength: String? = null,
    var form: String? = null,
    var frequency: String? = null,
    var duration: String? = null,
    var instructions: String? = null,
    var quantity: String? = null,
    val pregnancySafetyCategory: String = "unknown"
) {
    fun copy(pregnancySafetyCategory: String): MedicationInfo {
        return MedicationInfo(
            name = this.name,
            strength = this.strength,
            form = this.form,
            frequency = this.frequency,
            duration = this.duration,
            instructions = this.instructions,
            quantity = this.quantity,
            pregnancySafetyCategory = pregnancySafetyCategory
        )
    }
}

data class PrescriptionData(
    val medications: List<MedicationInfo>,
    val extractedText: String,
    val processingTimestamp: String
)

data class SafetyAlert(
    val medicationName: String,
    val level: SafetyAlertLevel,
    val message: String
)

enum class SafetyAlertLevel {
    LOW, MEDIUM, HIGH
}

/**
 * Result classes for prescription processing
 */
sealed class PrescriptionProcessingResult {
    data class Success(
        val prescriptionId: Long,
        val medications: List<MedicationInfo>,
        val extractedText: String,
        val safetyAlerts: List<SafetyAlert>
    ) : PrescriptionProcessingResult()
    
    data class Error(val message: String) : PrescriptionProcessingResult()
}