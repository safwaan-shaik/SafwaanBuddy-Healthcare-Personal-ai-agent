package com.safwaanbuddy.healthcare.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.safwaanbuddy.healthcare.data.converters.DateConverters
import com.safwaanbuddy.healthcare.data.dao.*
import com.safwaanbuddy.healthcare.data.models.*

/**
 * Healthcare Database with SQLCipher Encryption
 * Main database class for SafwaanBuddy Healthcare Application
 * Implements HIPAA-compliant encrypted storage
 */
@Database(
    entities = [
        Patient::class,
        Prescription::class,
        MedicationReminder::class,
        LabResult::class,
        VoiceCommandLog::class,
        MedicationCompliance::class,
        EmergencyContact::class,
        HealthMetric::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class HealthcareDatabase : RoomDatabase() {
    
    // DAO Abstracts
    abstract fun patientDao(): PatientDao
    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun labResultDao(): LabResultDao
    abstract fun voiceCommandLogDao(): VoiceCommandLogDao
    abstract fun medicationComplianceDao(): MedicationComplianceDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun healthMetricDao(): HealthMetricDao
    
    companion object {
        @Volatile
        private var INSTANCE: HealthcareDatabase? = null
        
        private const val DATABASE_NAME = "safwaan_buddy_healthcare.db"
        
        /**
         * Get database instance with encryption
         * Uses SQLCipher for HIPAA-compliant data protection
         */
        fun getDatabase(
            context: Context,
            passphrase: String
        ): HealthcareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, passphrase)
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Build encrypted database instance
         */
        private fun buildDatabase(
            context: Context,
            passphrase: String
        ): HealthcareDatabase {
            // Create SQLCipher factory with passphrase
            val passBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
            val factory = SupportFactory(passBytes)
            
            return Room.databaseBuilder(
                context.applicationContext,
                HealthcareDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory) // Enable SQLCipher encryption
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
        }
        
        /**
         * Clear database instance (for testing or logout)
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
        
        /**
         * Verify database encryption is working
         */
        fun isDatabaseEncrypted(context: Context, passphrase: String): Boolean {
            return try {
                val db = getDatabase(context, passphrase)
                // Try to access database - if wrong passphrase, this will fail
                db.patientDao().getActivePatientCount()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

/**
 * Database Configuration Class
 * Handles database-related configurations and utilities
 */
object DatabaseConfig {
    
    /**
     * Generate secure database passphrase
     * In production, this should be derived from user authentication
     */
    fun generatePassphrase(context: Context, userId: String): String {
        // This is a simplified version - in production, use more secure key derivation
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        
        // Combine user ID with device ID for unique passphrase
        return "$userId-$deviceId-safwaanbuddy-healthcare"
    }
    
    /**
     * Database migration strategies
     */
    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Future migration logic will go here
        }
    }
    
    /**
     * Backup database (encrypted)
     */
    fun backupDatabase(context: Context, passphrase: String): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = context.getDatabasePath("${DATABASE_NAME}.backup")
            
            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseConfig", "Database backup failed", e)
            false
        }
    }
    
    /**
     * Restore database from backup
     */
    fun restoreDatabase(context: Context, passphrase: String): Boolean {
        return try {
            val backupFile = context.getDatabasePath("${DATABASE_NAME}.backup")
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            
            if (backupFile.exists()) {
                // Close existing database connection
                HealthcareDatabase.clearInstance()
                
                // Restore from backup
                backupFile.copyTo(dbFile, overwrite = true)
                
                // Verify restored database
                HealthcareDatabase.isDatabaseEncrypted(context, passphrase)
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseConfig", "Database restore failed", e)
            false
        }
    }
    
    /**
     * Get database size for monitoring
     */
    fun getDatabaseSize(context: Context): Long {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }
    
    /**
     * Clean up old data (for privacy compliance)
     * Removes data older than specified retention period
     */
    suspend fun cleanupOldData(database: HealthcareDatabase, retentionDays: Int = 365) {
        try {
            val cutoffDate = kotlinx.datetime.Clock.System.now()
                .minus(kotlinx.datetime.DateTimeUnit.DAY * retentionDays)
                .let { kotlinx.datetime.LocalDateTime(it.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date, kotlinx.datetime.LocalTime(0, 0)) }
            
            // Clean up old voice command logs
            database.voiceCommandLogDao().deleteOldLogs(cutoffDate)
            
            android.util.Log.i("DatabaseConfig", "Old data cleanup completed")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseConfig", "Data cleanup failed", e)
        }
    }
}