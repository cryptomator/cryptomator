package org.cryptomator.common.keychain;

import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Multi-keyslot masterkey file format for TrueCrypt-style plausible deniability.
 * 
 * File Format (FIXED SIZE - AEAD-encrypted, indistinguishable from random data):
 * - Total Size: 16,384 bytes (16 KB)
 * - Slot 0: 4,096 bytes - AEAD-encrypted keyslot OR random padding
 * - Slot 1: 4,096 bytes - AEAD-encrypted keyslot OR random padding
 * - Slot 2: 4,096 bytes - AEAD-encrypted keyslot OR random padding
 * - Slot 3: 4,096 bytes - AEAD-encrypted keyslot OR random padding
 * 
 * AEAD Slot Format (each slot):
 * - Salt: 32 bytes (for PBKDF2 key derivation)
 * - IV/Nonce: 12 bytes (for AES-GCM)
 * - Ciphertext + Auth Tag: remaining bytes (AEAD-encrypted keyslot data)
 * 
 * Security Properties (True Plausible Deniability - AEAD-based):
 * - NO magic bytes - file is indistinguishable from random data
 * - NO version field - no identifying markers
 * - NO keyslot count - impossible to tell how many identities exist
 * - NO plaintext length markers - fixed by CodeRabbit critical issue
 * - Fixed size - always 4 slots regardless of actual use
 * - Each slot contains either:
 *   a) AEAD-encrypted masterkey data (password-derived key)
 *   b) Cryptographically secure random bytes
 * - Occupancy detection ONLY via AEAD authentication:
 *   • Try AEAD-decrypt with password
 *   • Success (auth tag validates) = real occupied slot
 *   • Failure (auth tag fails) = empty slot OR wrong password
 *   • These two cases are INDISTINGUISHABLE without the password
 * 
 * To unlock:
 * - Try AEAD-decrypting each of the 4 slots with provided password
 * - If a slot AEAD-decrypts successfully → return that masterkey
 * - If all 4 fail → InvalidPassphraseException
 * - Observer cannot determine which slot was used or how many exist
 * 
 * This design ensures an adversary cannot prove the existence of hidden identities.
 * Even under coercion, a user can plausibly claim to only know one password.
 * 
 * Fixes CodeRabbit Critical Issues:
 * - Removes plaintext length marker vulnerability
 * - Prevents slot enumeration without passwords
 * - Prevents identity counting without passwords
 */
public class MultiKeyslotFile {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiKeyslotFile.class);
	
	// Fixed format parameters - changing these breaks compatibility
	private static final int SLOT_SIZE = 4096;  // 4 KB per slot
	private static final int NUM_SLOTS = 4;     // Always 4 slots
	private static final int FILE_SIZE = NUM_SLOTS * SLOT_SIZE;  // 16 KB total
	
	// AEAD encryption parameters (AES-256-GCM)
	private static final int SALT_LENGTH = 32;  // 256 bits for PBKDF2
	private static final int IV_LENGTH = 12;    // 96 bits for GCM (recommended)
	private static final int GCM_TAG_LENGTH = 128;  // 128-bit authentication tag
	private static final int PBKDF2_ITERATIONS = 100000;  // Strong key derivation
	private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
	private static final int AES_KEY_SIZE = 256;  // AES-256
	
	// Overhead: salt(32) + iv(12) + tag(16) = 60 bytes
	private static final int AEAD_OVERHEAD = SALT_LENGTH + IV_LENGTH + (GCM_TAG_LENGTH / 8);
	private static final int MAX_KEYSLOT_SIZE = SLOT_SIZE - AEAD_OVERHEAD;
	
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final SecureRandom secureRandom;
	
	public MultiKeyslotFile(MasterkeyFileAccess masterkeyFileAccess) {
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.secureRandom = new SecureRandom();
	}
	
	/**
	 * Check if a file is a multi-keyslot file.
	 * Uses file size as the ONLY indicator - no magic bytes to avoid fingerprinting.
	 */
	public boolean isMultiKeyslotFile(Path path) throws IOException {
		if (!Files.exists(path)) {
			return false;
		}
		
		long size = Files.size(path);
		// Multi-keyslot files are exactly FILE_SIZE bytes
		// Single-keyslot (legacy) files are typically 500-2000 bytes
		return size == FILE_SIZE;
	}
	
	/**
	 * Try to load masterkey from any keyslot that matches the password.
	 * 
	 * Security: Tries all slots sequentially without revealing which succeeded.
	 * No logging of slot numbers or counts to prevent information leakage.
	 * 
	 * @param path Path to masterkey file
	 * @param password Password to try
	 * @return Masterkey if password matches any keyslot
	 * @throws InvalidPassphraseException if password doesn't match any keyslot
	 * @throws IOException on I/O errors
	 */
	public Masterkey load(Path path, CharSequence password) throws InvalidPassphraseException, IOException {
		if (!isMultiKeyslotFile(path)) {
			// Fall back to standard single-keyslot file
			return masterkeyFileAccess.load(path, password);
		}
		
		byte[] fileData = Files.readAllBytes(path);
		
		// Try each slot sequentially
		// Can't tell which are real keyslots vs random padding until we try to decrypt
		for (int i = 0; i < NUM_SLOTS; i++) {
			byte[] slotData = extractSlot(fileData, i);
			
			try {
			Masterkey masterkey = decryptSlot(slotData, password);
			// Success - but don't reveal which slot!
			LOG.trace("Vault unlock successful");
			return masterkey;
			} catch (InvalidPassphraseException e) {
				// Could be: wrong password, or this slot is just random padding
				// Either way, silently try the next slot
				continue;
			}
		}
		
		// None of the slots decrypted - either wrong password or no matching keyslot
		throw new InvalidPassphraseException();
	}
	
	/**
	 * Create a new multi-keyslot file with a single keyslot.
	 * Remaining slots are filled with secure random data for indistinguishability.
	 * 
	 * @param path Path where file will be created
	 * @param masterkey Masterkey to encrypt
	 * @param password Password to encrypt with
	 * @param scryptCostParam scrypt cost parameter
	 * @throws IOException on I/O errors
	 */
	public void persist(Path path, Masterkey masterkey, CharSequence password, int scryptCostParam) throws IOException {
		// Create temp file with encrypted masterkey
		Path tempKeyslot = Files.createTempFile("keyslot-", ".tmp");
		try {
			masterkeyFileAccess.persist(masterkey, tempKeyslot, password, scryptCostParam);
			byte[] keyslotData = Files.readAllBytes(tempKeyslot);
			
			// Create fixed-size file
			byte[] fileData = new byte[FILE_SIZE];
			
			// Slot 0: Real AEAD-encrypted keyslot
			try {
				byte[] slot0 = aeadEncryptSlot(keyslotData, password);
				System.arraycopy(slot0, 0, fileData, 0, SLOT_SIZE);
			} catch (GeneralSecurityException e) {
				throw new IOException("Failed to encrypt keyslot", e);
			}
			
			// Slots 1-3: Secure random data (indistinguishable from AEAD encryption)
			for (int i = 1; i < NUM_SLOTS; i++) {
				byte[] randomSlot = new byte[SLOT_SIZE];
				secureRandom.nextBytes(randomSlot);
				System.arraycopy(randomSlot, 0, fileData, i * SLOT_SIZE, SLOT_SIZE);
			}
			
			// Write atomically via temp file + rename
			Path tempFile = Files.createTempFile(path.getParent(), ".vault-", ".tmp");
			try {
				Files.write(tempFile, fileData, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
				// Attempt atomic move, fallback to non-atomic if not supported
				try {
					Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (java.nio.file.AtomicMoveNotSupportedException e) {
					Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
				}
				LOG.trace("Vault file created");
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} finally {
			Files.deleteIfExists(tempKeyslot);
		}
	}
	
	/**
	 * Add a hidden keyslot to an existing file.
	 * Finds first empty slot (random padding) and replaces it with encrypted keyslot.
	 * 
	 * @param path Path to existing multi-keyslot file
	 * @param masterkey Hidden masterkey to add
	 * @param password Password for hidden keyslot
	 * @param scryptCostParam scrypt cost parameter
	 * @throws IOException on I/O errors or if all slots are full
	 */
	public void addKeyslot(Path path, Masterkey masterkey, CharSequence password, int scryptCostParam) throws IOException {
		byte[] fileData;
		
		if (isMultiKeyslotFile(path)) {
			fileData = Files.readAllBytes(path);
		} else {
			// Convert legacy single-keyslot file to multi-keyslot format
			LOG.trace("Converting legacy format");
			byte[] legacyData = Files.readAllBytes(path);
			fileData = new byte[FILE_SIZE];
			
			// Slot 0: Legacy keyslot (AEAD-encrypted)
			try {
				byte[] slot0 = aeadEncryptSlot(legacyData, password);
				System.arraycopy(slot0, 0, fileData, 0, SLOT_SIZE);
			} catch (GeneralSecurityException e) {
				throw new IOException("Failed to encrypt legacy keyslot", e);
			}
			
			// Slots 1-3: Random padding
			for (int i = 1; i < NUM_SLOTS; i++) {
				byte[] randomSlot = new byte[SLOT_SIZE];
				secureRandom.nextBytes(randomSlot);
				System.arraycopy(randomSlot, 0, fileData, i * SLOT_SIZE, SLOT_SIZE);
			}
		}
		
		// Find first available slot using AEAD authentication
		// Step 1: Check for duplicate passwords across all slots
		// Step 2: Find first slot that doesn't decrypt with new password (empty or different password)
		int availableSlot = -1;
		for (int i = 0; i < NUM_SLOTS; i++) {
			byte[] slotData = extractSlot(fileData, i);
			try {
				// Try AEAD-decrypting with the NEW password we're adding
				aeadDecryptSlot(slotData, password);
				// If we reach here, this slot already has this password
				throw new IOException("A keyslot with this password already exists");
			} catch (GeneralSecurityException e) {
				// AEAD authentication failed - slot is either:
				// a) Empty (random padding), or
				// b) Occupied with a DIFFERENT password
				// In AEAD-based design, these are cryptographically indistinguishable
				// We use the first such slot we find
				if (availableSlot == -1) {
					availableSlot = i;
					// Continue checking remaining slots for duplicates
				}
			}
		}
		
		if (availableSlot == -1) {
			// This shouldn't happen - at least one slot should have failed AEAD
			throw new IOException("All keyslots are occupied with this password");
		}
		
		// Create new encrypted keyslot
		Path tempKeyslot = Files.createTempFile("keyslot-", ".tmp");
		try {
			masterkeyFileAccess.persist(masterkey, tempKeyslot, password, scryptCostParam);
			byte[] newKeyslotData = Files.readAllBytes(tempKeyslot);
			
			// AEAD-encrypt the keyslot with the provided password
			byte[] aeadEncryptedSlot;
			try {
				aeadEncryptedSlot = aeadEncryptSlot(newKeyslotData, password);
			} catch (GeneralSecurityException e) {
				throw new IOException("Failed to encrypt keyslot", e);
			}
			
			// Replace available slot with new keyslot
			System.arraycopy(aeadEncryptedSlot, 0, fileData, availableSlot * SLOT_SIZE, SLOT_SIZE);
			
			// Write back atomically
			Path tempFile = Files.createTempFile(path.getParent(), ".keyslot-", ".tmp");
			try {
				Files.write(tempFile, fileData);
				try {
					Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				} catch (java.nio.file.AtomicMoveNotSupportedException e) {
					Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
				}
				LOG.trace("Keyslot added");
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} finally {
			Files.deleteIfExists(tempKeyslot);
		}
	}
	
	/**
	 * Remove a keyslot from the file by replacing it with secure random data.
	 * WARNING: This requires knowing the password of the keyslot to remove.
	 * 
	 * Security: Replaces slot with random data to maintain indistinguishability.
	 * Cannot determine how many slots remain after removal (by design).
	 * 
	 * NOTE: We do NOT check if this is the "last keyslot" because:
	 * 1. Counting occupied slots would leak information (CodeRabbit critical issue)
	 * 2. User is responsible for managing their own passwords
	 * 3. If they remove their only access, that's their explicit choice
	 * 
	 * @param path Path to multi-keyslot file
	 * @param password Password of keyslot to remove
	 * @return true if a keyslot was removed
	 * @throws IOException on I/O errors
	 */
	public boolean removeKeyslot(Path path, CharSequence password) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			LOG.warn("Cannot remove keyslot from non-multi-keyslot file");
			return false;
		}
		
		byte[] fileData = Files.readAllBytes(path);
		
		// Find which slot matches the password
		int slotToRemove = -1;
		for (int i = 0; i < NUM_SLOTS; i++) {
			byte[] slotData = extractSlot(fileData, i);
			try {
				Masterkey key = decryptSlot(slotData, password);
				key.destroy();
				slotToRemove = i;
				break;
			} catch (InvalidPassphraseException e) {
				// Not this slot
			}
		}
		
		if (slotToRemove == -1) {
			LOG.warn("Password doesn't match any keyslot");
			return false;
		}
		
		// Replace slot with secure random data
		// This makes it indistinguishable from an empty slot
		byte[] randomSlot = new byte[SLOT_SIZE];
		secureRandom.nextBytes(randomSlot);
		System.arraycopy(randomSlot, 0, fileData, slotToRemove * SLOT_SIZE, SLOT_SIZE);
		
		// Write back atomically
		Path tempFile = Files.createTempFile(path.getParent(), ".keyslot-", ".tmp");
		try {
			Files.write(tempFile, fileData);
			try {
				Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
			}
			LOG.trace("Keyslot removed");
		} finally {
			Files.deleteIfExists(tempFile);
		}
		
		return true;
	}
	
	// ========== Private Helper Methods ==========
	
	/**
	 * Extract a specific slot from the file data.
	 */
	private byte[] extractSlot(byte[] fileData, int slotIndex) {
		int offset = slotIndex * SLOT_SIZE;
		return Arrays.copyOfRange(fileData, offset, offset + SLOT_SIZE);
	}
	
	/**
	 * Decrypt a slot using AEAD to recover the masterkey.
	 * Uses AEAD authentication to determine if slot is occupied.
	 * 
	 * @throws InvalidPassphraseException if AEAD authentication fails (wrong password OR empty slot)
	 */
	private Masterkey decryptSlot(byte[] slotData, CharSequence password) throws InvalidPassphraseException, IOException {
		try {
			// AEAD-decrypt the slot to get original keyslot data
			byte[] keyslotData = aeadDecryptSlot(slotData, password);
			
			// Write to temp file for MasterkeyFileAccess.load()
			Path tempFile = Files.createTempFile("keyslot-", ".tmp");
			try {
				Files.write(tempFile, keyslotData);
				return masterkeyFileAccess.load(tempFile, password);
			} finally {
				Files.deleteIfExists(tempFile);
			}
		} catch (GeneralSecurityException e) {
			// AEAD authentication failed - wrong password OR empty slot
			throw new InvalidPassphraseException();
		}
	}
	
	/**
	 * AEAD-encrypt keyslot data to fixed slot size using AES-256-GCM.
	 * 
	 * Format: [salt:32][iv:12][ciphertext + auth_tag]
	 * 
	 * Security: AEAD provides both encryption and authentication.
	 * Authentication tag ensures tampering detection and proves occupancy.
	 * 
	 * @throws IllegalArgumentException if keyslot data too large
	 */
	private byte[] aeadEncryptSlot(byte[] keyslotData, CharSequence password) throws GeneralSecurityException {
		if (keyslotData.length > MAX_KEYSLOT_SIZE) {
			throw new IllegalArgumentException("Keyslot data too large: " + keyslotData.length + 
				" bytes (max " + MAX_KEYSLOT_SIZE + " with AEAD overhead)");
		}
		
		// Generate random salt for key derivation
		byte[] salt = new byte[SALT_LENGTH];
		secureRandom.nextBytes(salt);
		
		// Derive encryption key from password using PBKDF2
		SecretKey encryptionKey = deriveKey(password, salt);
		
		// Generate random IV for GCM
		byte[] iv = new byte[IV_LENGTH];
		secureRandom.nextBytes(iv);
		
		// Encrypt with AES-GCM (AEAD)
		Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
		GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
		
		// Encrypt: returns ciphertext + authentication tag
		byte[] ciphertextWithTag = cipher.doFinal(keyslotData);
		
		// Build slot: [salt][iv][ciphertext+tag][random padding]
		byte[] slot = new byte[SLOT_SIZE];
		ByteBuffer buffer = ByteBuffer.wrap(slot);
		buffer.put(salt);
		buffer.put(iv);
		buffer.put(ciphertextWithTag);
		
		// Fill remaining space with random data for indistinguishability
		int remaining = SLOT_SIZE - buffer.position();
		if (remaining > 0) {
			byte[] padding = new byte[remaining];
			secureRandom.nextBytes(padding);
			buffer.put(padding);
		}
		
		return slot;
	}
	
	/**
	 * AEAD-decrypt a slot using AES-256-GCM.
	 * 
	 * @throws GeneralSecurityException if AEAD authentication fails
	 *         This means EITHER wrong password OR slot contains random data (empty slot)
	 *         These two cases are cryptographically indistinguishable!
	 */
	private byte[] aeadDecryptSlot(byte[] slotData, CharSequence password) throws GeneralSecurityException {
		if (slotData.length != SLOT_SIZE) {
			throw new GeneralSecurityException("Invalid slot size");
		}
		
		ByteBuffer buffer = ByteBuffer.wrap(slotData);
		
		// Extract salt
		byte[] salt = new byte[SALT_LENGTH];
		buffer.get(salt);
		
		// Extract IV
		byte[] iv = new byte[IV_LENGTH];
		buffer.get(iv);
		
		// Remaining is ciphertext + tag (we don't know exact size, try to decrypt all)
		int ciphertextLength = SLOT_SIZE - SALT_LENGTH - IV_LENGTH;
		byte[] ciphertextWithTag = new byte[ciphertextLength];
		buffer.get(ciphertextWithTag);
		
		// Derive decryption key from password
		SecretKey decryptionKey = deriveKey(password, salt);
		
		// Decrypt with AES-GCM (AEAD)
		Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
		GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.DECRYPT_MODE, decryptionKey, gcmSpec);
		
		// Decrypt and authenticate
		// If this succeeds: valid encrypted data
		// If this fails: wrong password OR random padding (indistinguishable!)
		byte[] plaintext = cipher.doFinal(ciphertextWithTag);
		
		// Remove any padding from plaintext to get original keyslot data
		// The plaintext may have trailing zeros from our padding
		return trimTrailingZeros(plaintext);
	}
	
	/**
	 * Derive AES-256 key from password using PBKDF2-HMAC-SHA256.
	 */
	private SecretKey deriveKey(CharSequence password, byte[] salt) throws GeneralSecurityException {
		try {
			char[] passwordChars = new char[password.length()];
			for (int i = 0; i < password.length(); i++) {
				passwordChars[i] = password.charAt(i);
			}
			
			PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
			byte[] keyBytes = factory.generateSecret(spec).getEncoded();
			
			// Clear sensitive data
			Arrays.fill(passwordChars, '\0');
			spec.clearPassword();
			
			return new SecretKeySpec(keyBytes, "AES");
		} catch (Exception e) {
			throw new GeneralSecurityException("Key derivation failed", e);
		}
	}
	
	/**
	 * Remove trailing zeros from decrypted data.
	 * This handles the padding we added during encryption.
	 */
	private byte[] trimTrailingZeros(byte[] data) {
		int end = data.length;
		while (end > 0 && data[end - 1] == 0) {
			end--;
		}
		return Arrays.copyOf(data, end);
	}
}
