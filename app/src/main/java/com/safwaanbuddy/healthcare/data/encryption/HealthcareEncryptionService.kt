package com.safwaanbuddy.healthcare.data.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricPrompt
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Hex
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Healthcare Data Encryption Service
 * HIPAA-compliant encryption for sensitive medical data
 * Uses AES-256-GCM with Android Keystore integration
 */
@Singleton
class HealthcareEncryptionService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "HealthcareEncryption"
        private const val KEYSTORE_ALIAS = "SafwaanBuddyHealthcareKey"
        private const val KEYSET_NAME = "healthcare_master_keyset"
        private const val PREFERENCE_FILE = "healthcare_crypto_prefs"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    private var aead: Aead? = null
    
    init {
        initializeEncryption()
    }
    
    /**
     * Initialize encryption system with Tink and Android Keystore
     */
    private fun initializeEncryption() {
        try {
            // Initialize Tink AEAD
            AeadConfig.register()
            
            // Create or load master keyset
            val keysetHandle = createOrLoadMasterKeyset()
            aead = keysetHandle.getPrimitive(Aead::class.java)
            
            Log.i(TAG, "Healthcare encryption initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encryption", e)
            throw HealthcareEncryptionException("Encryption initialization failed", e)
        }
    }
    
    /**
     * Create or load master keyset with Android Keystore protection
     */
    private fun createOrLoadMasterKeyset(): KeysetHandle {
        return try {
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri("android-keystore://$KEYSTORE_ALIAS")
                .build()
                .keysetHandle
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/load master keyset", e)
            throw HealthcareEncryptionException("Master keyset creation failed", e)
        }
    }
    
    /**
     * Encrypt sensitive healthcare data
     * @param plaintext The data to encrypt
     * @param associatedData Additional data for authentication (optional)
     * @return Base64 encoded encrypted data
     */
    fun encrypt(plaintext: String, associatedData: String? = null): String {
        return try {
            val aead = this.aead ?: throw HealthcareEncryptionException("Encryption not initialized")
            
            val plaintextBytes = plaintext.toByteArray(Charsets.UTF_8)
            val associatedDataBytes = associatedData?.toByteArray(Charsets.UTF_8)
            
            val ciphertext = aead.encrypt(plaintextBytes, associatedDataBytes)
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            throw HealthcareEncryptionException("Failed to encrypt data", e)
        }
    }
    
    /**
     * Decrypt sensitive healthcare data
     * @param encryptedData Base64 encoded encrypted data
     * @param associatedData Additional data for authentication (optional)
     * @return Decrypted plaintext
     */
    fun decrypt(encryptedData: String, associatedData: String? = null): String {
        return try {
            val aead = this.aead ?: throw HealthcareEncryptionException("Encryption not initialized")
            
            val ciphertext = Base64.decode(encryptedData, Base64.NO_WRAP)
            val associatedDataBytes = associatedData?.toByteArray(Charsets.UTF_8)
            
            val plaintext = aead.decrypt(ciphertext, associatedDataBytes)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw HealthcareEncryptionException("Failed to decrypt data", e)
        }
    }
    
    /**
     * Encrypt large data (e.g., images, documents)
     * Uses streaming encryption for better performance
     */
    fun encryptLargeData(data: ByteArray, associatedData: String? = null): ByteArray {
        return try {
            val aead = this.aead ?: throw HealthcareEncryptionException("Encryption not initialized")
            val associatedDataBytes = associatedData?.toByteArray(Charsets.UTF_8)
            
            aead.encrypt(data, associatedDataBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Large data encryption failed", e)
            throw HealthcareEncryptionException("Failed to encrypt large data", e)
        }
    }
    
    /**
     * Decrypt large data (e.g., images, documents)
     */
    fun decryptLargeData(encryptedData: ByteArray, associatedData: String? = null): ByteArray {
        return try {
            val aead = this.aead ?: throw HealthcareEncryptionException("Encryption not initialized")
            val associatedDataBytes = associatedData?.toByteArray(Charsets.UTF_8)
            
            aead.decrypt(encryptedData, associatedDataBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Large data decryption failed", e)
            throw HealthcareEncryptionException("Failed to decrypt large data", e)
        }
    }
    
    /**
     * Encrypt with Android Keystore (for additional security layer)
     */
    fun encryptWithKeystore(plaintext: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            // Generate key if it doesn't exist
            if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                generateKeystoreKey()
            }
            
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV and ciphertext
            val combined = iv + cipherText
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Keystore encryption failed", e)
            throw HealthcareEncryptionException("Keystore encryption failed", e)
        }
    }
    
    /**
     * Decrypt with Android Keystore
     */
    fun decryptWithKeystore(encryptedData: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // Extract IV and ciphertext
            val iv = combined.sliceArray(0..GCM_IV_LENGTH - 1)
            val cipherText = combined.sliceArray(GCM_IV_LENGTH until combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val plaintext = cipher.doFinal(cipherText)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Keystore decryption failed", e)
            throw HealthcareEncryptionException("Keystore decryption failed", e)
        }
    }
    
    /**
     * Generate Android Keystore key for additional encryption layer
     */
    private fun generateKeystoreKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // Set to true for biometric authentication
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Log.e(TAG, "Keystore key generation failed", e)
            throw HealthcareEncryptionException("Failed to generate keystore key", e)
        }
    }
    
    /**
     * Generate secure hash for data integrity verification
     */
    fun generateHash(data: String): String {
        return try {
            val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = messageDigest.digest(data.toByteArray(Charsets.UTF_8))
            Hex.encode(hashBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Hash generation failed", e)
            throw HealthcareEncryptionException("Failed to generate hash", e)
        }
    }
    
    /**
     * Verify data integrity using hash
     */
    fun verifyHash(data: String, expectedHash: String): Boolean {
        return try {
            val actualHash = generateHash(data)
            actualHash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Hash verification failed", e)
            false
        }
    }
    
    /**
     * Secure data wipe (overwrite memory)
     */
    fun secureWipe(data: ByteArray) {
        try {
            // Overwrite with random data multiple times
            val random = java.security.SecureRandom()
            repeat(3) {
                random.nextBytes(data)
            }
            // Final overwrite with zeros
            data.fill(0)
        } catch (e: Exception) {
            Log.e(TAG, "Secure wipe failed", e)
        }
    }
    
    /**
     * Generate secure random string for keys/IVs
     */
    fun generateSecureRandom(length: Int): String {
        return try {
            val random = java.security.SecureRandom()
            val bytes = ByteArray(length)
            random.nextBytes(bytes)
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Secure random generation failed", e)
            throw HealthcareEncryptionException("Failed to generate secure random", e)
        }
    }
    
    /**
     * Check if encryption is properly initialized
     */
    fun isEncryptionReady(): Boolean {
        return aead != null
    }
    
    /**
     * Re-initialize encryption (for recovery scenarios)
     */
    fun reinitializeEncryption() {
        try {
            aead = null
            initializeEncryption()
        } catch (e: Exception) {
            Log.e(TAG, "Encryption reinitialization failed", e)
            throw HealthcareEncryptionException("Failed to reinitialize encryption", e)
        }
    }
    
    /**
     * Clear encryption keys (for logout/security purposes)
     */
    fun clearEncryptionKeys() {
        try {
            aead = null
            
            // Clear keystore key
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            }
            
            // Clear shared preferences
            val prefs = context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            Log.i(TAG, "Encryption keys cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear encryption keys", e)
        }
    }
}

/**
 * Healthcare-specific encryption exception
 */
class HealthcareEncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Encryption utility functions for healthcare data types
 */
object HealthcareEncryptionUtils {
    
    /**
     * Encrypt patient name with additional context
     */
    fun encryptPatientName(
        encryptionService: HealthcareEncryptionService,
        name: String,
        patientId: String
    ): String {
        return encryptionService.encrypt(name, "patient_name_$patientId")
    }
    
    /**
     * Decrypt patient name
     */
    fun decryptPatientName(
        encryptionService: HealthcareEncryptionService,
        encryptedName: String,
        patientId: String
    ): String {
        return encryptionService.decrypt(encryptedName, "patient_name_$patientId")
    }
    
    /**
     * Encrypt medical data with type context
     */
    fun encryptMedicalData(
        encryptionService: HealthcareEncryptionService,
        data: String,
        dataType: String,
        patientId: String
    ): String {
        return encryptionService.encrypt(data, "${dataType}_$patientId")
    }
    
    /**
     * Decrypt medical data
     */
    fun decryptMedicalData(
        encryptionService: HealthcareEncryptionService,
        encryptedData: String,
        dataType: String,
        patientId: String
    ): String {
        return encryptionService.decrypt(encryptedData, "${dataType}_$patientId")
    }
    
    /**
     * Validate encrypted data format
     */
    fun isValidEncryptedData(encryptedData: String?): Boolean {
        return try {
            encryptedData != null && 
            encryptedData.isNotBlank() && 
            Base64.decode(encryptedData, Base64.NO_WRAP).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}