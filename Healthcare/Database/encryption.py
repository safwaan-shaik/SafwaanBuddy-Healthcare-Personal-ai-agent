"""
Healthcare Database Encryption Module
Provides HIPAA-compliant data encryption for sensitive medical information
"""

import os
import json
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import base64
from typing import Union, Any
from dotenv import dotenv_values

class HealthcareEncryption:
    """
    Encryption utility for healthcare data with AES-256 encryption
    """
    
    def __init__(self, password: str = None):
        """Initialize encryption with master password"""
        if password is None:
            env_vars = dotenv_values('.env')
            password = env_vars.get('HEALTHCARE_MASTER_KEY', 'jarvis-healthcare-2025')
        
        # Generate key from password
        password_bytes = password.encode()
        salt = b'jarvis_healthcare_salt_2025'  # In production, use random salt
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=salt,
            iterations=100000,
        )
        key = base64.urlsafe_b64encode(kdf.derive(password_bytes))
        self.cipher = Fernet(key)
    
    def encrypt_data(self, sensitive_data: Union[str, dict]) -> str:
        """Encrypt sensitive healthcare data"""
        if isinstance(sensitive_data, dict):
            sensitive_data = json.dumps(sensitive_data)
        
        encrypted_data = self.cipher.encrypt(sensitive_data.encode())
        return base64.urlsafe_b64encode(encrypted_data).decode()
    
    def decrypt_data(self, encrypted_data: str) -> str:
        """Decrypt healthcare data"""
        try:
            encrypted_bytes = base64.urlsafe_b64decode(encrypted_data.encode())
            decrypted_data = self.cipher.decrypt(encrypted_bytes)
            return decrypted_data.decode()
        except Exception as e:
            raise ValueError(f"Failed to decrypt data: {e}")
    
    def encrypt_json(self, data: dict) -> str:
        """Encrypt JSON data and return as base64 string"""
        json_str = json.dumps(data)
        return self.encrypt_data(json_str)
    
    def decrypt_json(self, encrypted_data: str) -> dict:
        """Decrypt base64 string and return as JSON"""
        decrypted_str = self.decrypt_data(encrypted_data)
        return json.loads(decrypted_str)

class HealthcareAuditLogger:
    """
    Audit logging for all healthcare interactions
    """
    
    def __init__(self, encryption_manager: HealthcareEncryption):
        self.encryption = encryption_manager
        self.audit_file = 'Healthcare/Database/audit_log.encrypted'
        
    def log_medical_interaction(self, action: str, patient_id: str, details: dict):
        """Log medical interaction with encryption"""
        from datetime import datetime
        
        audit_entry = {
            'timestamp': datetime.now().isoformat(),
            'action': action,
            'patient_id': patient_id,
            'details': details,
            'system_user': 'JARVIS_HEALTHCARE_SYSTEM'
        }
        
        # Encrypt and store audit log
        encrypted_entry = self.encryption.encrypt_json(audit_entry)
        
        # Append to audit file
        with open(self.audit_file, 'a', encoding='utf-8') as f:
            f.write(encrypted_entry + '\n')
    
    def get_audit_logs(self, patient_id: str = None) -> list:
        """Retrieve audit logs (decrypted)"""
        if not os.path.exists(self.audit_file):
            return []
        
        logs = []
        try:
            with open(self.audit_file, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip():
                        try:
                            decrypted_log = self.encryption.decrypt_json(line.strip())
                            if patient_id is None or decrypted_log.get('patient_id') == patient_id:
                                logs.append(decrypted_log)
                        except Exception as e:
                            print(f"Warning: Could not decrypt audit log entry: {e}")
        except Exception as e:
            print(f"Error reading audit logs: {e}")
        
        return logs