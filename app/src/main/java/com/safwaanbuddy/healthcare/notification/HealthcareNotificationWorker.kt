package com.safwaanbuddy.healthcare.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safwaanbuddy.healthcare.data.repository.MedicationRepository
import com.safwaanbuddy.healthcare.data.repository.PatientRepository
import com.safwaanbuddy.healthcare.voice.HealthcareAiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class HealthcareNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val patientRepository: PatientRepository,
    private val medicationRepository: MedicationRepository,
    private val aiService: HealthcareAiService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get all patients
            val patients = patientRepository.getAllPatients()
            
            for (patient in patients) {
                // Generate a personalized check-in message
                val checkInMessage = aiService.generateProactiveCheckIn(patient.id).await()
                
                // Check for missed medications
                val missedMeds = medicationRepository.getMissedMedications(patient.id)
                val reminderMessage = if (missedMeds.isNotEmpty()) {
                    aiService.generateMotivationalMessage(patient.id).await()
                } else {
                    null
                }
                
                // Send notifications
                NotificationHelper.sendHealthCheckNotification(
                    applicationContext,
                    patient.id,
                    checkInMessage
                )
                
                reminderMessage?.let {
                    NotificationHelper.sendMedicationReminder(
                        applicationContext,
                        patient.id,
                        it
                    )
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}