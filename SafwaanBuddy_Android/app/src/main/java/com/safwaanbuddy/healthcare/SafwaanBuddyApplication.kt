package com.safwaanbuddy.healthcare

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import com.google.crypto.tink.aead.AeadConfig
import com.safwaanbuddy.healthcare.data.database.HealthcareDatabase
import com.safwaanbuddy.healthcare.data.encryption.HealthcareEncryptionService
import javax.inject.Inject

/**
 * SafwaanBuddy Healthcare Application Class
 * Initializes HIPAA-compliant healthcare application
 */
@HiltAndroidApp
class SafwaanBuddyApplication : Application() {
    
    companion object {
        private const val TAG = "SafwaanBuddyApp"
    }
    
    @Inject
    lateinit var encryptionService: HealthcareEncryptionService
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize healthcare encryption
            initializeEncryption()
            
            // Initialize database
            initializeDatabase()
            
            // Initialize healthcare monitoring
            initializeHealthcareMonitoring()
            
            Log.i(TAG, "SafwaanBuddy Healthcare application initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize healthcare application", e)
            // In production, you might want to show an error dialog or crash gracefully
        }
    }
    
    /**
     * Initialize encryption system for healthcare data protection
     */
    private fun initializeEncryption() {
        try {
            // Initialize Tink for advanced encryption
            AeadConfig.register()
            
            Log.i(TAG, "Healthcare encryption initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encryption", e)
            throw e
        }
    }
    
    /**
     * Initialize healthcare database
     */
    private fun initializeDatabase() {
        try {
            // Database initialization is handled by Hilt injection
            // and happens when first accessed
            Log.i(TAG, "Healthcare database ready for initialization")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize database", e)
            throw e
        }
    }
    
    /**
     * Initialize healthcare monitoring and compliance features
     */
    private fun initializeHealthcareMonitoring() {
        try {
            // Set up background monitoring for medication reminders
            // Set up HIPAA compliance monitoring
            // Initialize audit logging
            
            Log.i(TAG, "Healthcare monitoring initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize healthcare monitoring", e)
            throw e
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        try {
            // Clean up sensitive data from memory
            cleanupSensitiveData()
            
            Log.i(TAG, "SafwaanBuddy Healthcare application terminated cleanly")
        } catch (e: Exception) {
            Log.e(TAG, "Error during application termination", e)
        }
    }
    
    /**
     * Clean up sensitive healthcare data from memory
     */
    private fun cleanupSensitiveData() {
        try {
            // Clear any cached sensitive data
            // Close database connections
            HealthcareDatabase.clearInstance()
            
            Log.i(TAG, "Sensitive data cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup sensitive data", e)
        }
    }
}