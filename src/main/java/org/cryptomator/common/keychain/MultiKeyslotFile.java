package org.cryptomator.common.keychain;

import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Multi-keyslot masterkey file format for TrueCrypt-style plausible deniability.
 * 
 * File Format (FIXED SIZE - indistinguishable from random data):
 * - Total Size: 16,384 bytes (16 KB)
 * - Slot 0: 4,096 bytes - encrypted keyslot OR random padding
 * - Slot 1: 4,096 bytes - encrypted keyslot OR random padding
 * - Slot 2: 4,096 bytes - encrypted keyslot OR random padding
 * - Slot 3: 4,096 bytes - encrypted keyslot OR random padding
 * 
 * Security Properties (True Plausible Deniability):
 * - NO magic bytes - file is indistinguishable from random data
 * - NO version field - no identifying markers
 * - NO keyslot count - impossible to tell how many identities exist
 * - Fixed size - always 4 slots regardless of actual use
 * - Each slot contains either:
 *   a) Password-encrypted masterkey file data (padded to slot size)
 *   b) Cryptographically secure random bytes (indistinguishable from encryption)
 * 
 * To unlock:
 * - Try decrypting each of the 4 slots with provided password
 * - If a slot decrypts successfully → return that masterkey
 * - If all 4 fail → InvalidPassphraseException
 * - Observer cannot determine which slot was used or how many exist
 * 
 * This design ensures an adversary cannot prove the existence of hidden identities.
 * Even under coercion, a user can plausibly claim to only know one password.
 */
public class MultiKeyslotFile {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiKeyslotFile.class);
	
	// Fixed format parameters - changing these breaks compatibility
	private static final int SLOT_SIZE = 4096;  // 4 KB per slot
	private static final int NUM_SLOTS = 4;     // Always 4 slots
	private static final int FILE_SIZE = NUM_SLOTS * SLOT_SIZE;  // 16 KB total
	
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
			
			// Slot 0: Real encrypted keyslot (padded to SLOT_SIZE)
			byte[] slot0 = padSlot(keyslotData);
			System.arraycopy(slot0, 0, fileData, 0, SLOT_SIZE);
			
			// Slots 1-3: Secure random data (indistinguishable from encryption)
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
			
			// Slot 0: Legacy keyslot (padded)
			byte[] slot0 = padSlot(legacyData);
			System.arraycopy(slot0, 0, fileData, 0, SLOT_SIZE);
			
			// Slots 1-3: Random padding
			for (int i = 1; i < NUM_SLOTS; i++) {
				byte[] randomSlot = new byte[SLOT_SIZE];
				secureRandom.nextBytes(randomSlot);
				System.arraycopy(randomSlot, 0, fileData, i * SLOT_SIZE, SLOT_SIZE);
			}
		}
		
	// Find first empty slot using unpadding detection
	// Empty slots contain random data and fail unpadding validation
	int emptySlot = findFirstEmptySlot(fileData);
		
		if (emptySlot == -1) {
			throw new IOException("All keyslots are full (maximum " + NUM_SLOTS + " identities)");
		}
		
		// Create new encrypted keyslot
		Path tempKeyslot = Files.createTempFile("keyslot-", ".tmp");
		try {
			masterkeyFileAccess.persist(masterkey, tempKeyslot, password, scryptCostParam);
			byte[] newKeyslotData = Files.readAllBytes(tempKeyslot);
			byte[] paddedSlot = padSlot(newKeyslotData);
			
			// Replace empty slot with new keyslot
			System.arraycopy(paddedSlot, 0, fileData, emptySlot * SLOT_SIZE, SLOT_SIZE);
			
			// Write back atomically
			Path tempFile = Files.createTempFile(path.getParent(), ".keyslot-", ".tmp");
			try {
				Files.write(tempFile, fileData);
				Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
	 * Cannot determine how many slots remain after removal.
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
		
		// Check if this is the last keyslot
		int occupiedSlots = countOccupiedSlots(fileData);
		if (occupiedSlots <= 1) {
			LOG.warn("Cannot remove last keyslot");
			return false;
		}
		
		// Replace slot with secure random data
		byte[] randomSlot = new byte[SLOT_SIZE];
		secureRandom.nextBytes(randomSlot);
		System.arraycopy(randomSlot, 0, fileData, slotToRemove * SLOT_SIZE, SLOT_SIZE);
		
		// Write back atomically
		Path tempFile = Files.createTempFile(path.getParent(), ".keyslot-", ".tmp");
		try {
			Files.write(tempFile, fileData);
			Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
	 * Decrypt a slot to recover the masterkey.
	 * @throws InvalidPassphraseException if decryption fails (wrong password or random padding)
	 */
	private Masterkey decryptSlot(byte[] slotData, CharSequence password) throws InvalidPassphraseException, IOException {
		// Unpad the slot to get original keyslot data
		byte[] keyslotData = unpadSlot(slotData);
		
		// Write to temp file for decryption
		Path tempFile = Files.createTempFile("keyslot-", ".tmp");
		try {
			Files.write(tempFile, keyslotData);
			return masterkeyFileAccess.load(tempFile, password);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}
	
	/**
	 * Pad keyslot data to fixed slot size.
	 * Padding format: [actual data][4-byte length][random padding to fill SLOT_SIZE]
	 */
	private byte[] padSlot(byte[] keyslotData) {
		if (keyslotData.length > SLOT_SIZE - 4) {
			throw new IllegalArgumentException("Keyslot data too large: " + keyslotData.length + " bytes (max " + (SLOT_SIZE - 4) + ")");
		}
		
		byte[] padded = new byte[SLOT_SIZE];
		
		// Copy actual data at the start
		System.arraycopy(keyslotData, 0, padded, 0, keyslotData.length);
		
		// Store length at position after data (little-endian)
		int lengthPos = keyslotData.length;
		padded[lengthPos] = (byte) (keyslotData.length & 0xFF);
		padded[lengthPos + 1] = (byte) ((keyslotData.length >> 8) & 0xFF);
		padded[lengthPos + 2] = (byte) ((keyslotData.length >> 16) & 0xFF);
		padded[lengthPos + 3] = (byte) ((keyslotData.length >> 24) & 0xFF);
		
		// Fill remainder with random data
		if (lengthPos + 4 < SLOT_SIZE) {
			byte[] randomPadding = new byte[SLOT_SIZE - lengthPos - 4];
			secureRandom.nextBytes(randomPadding);
			System.arraycopy(randomPadding, 0, padded, lengthPos + 4, randomPadding.length);
		}
		
		return padded;
	}
	
	/**
	 * Unpad a slot to recover original keyslot data.
	 * @throws InvalidPassphraseException if slot appears to be random padding
	 */
	private byte[] unpadSlot(byte[] paddedSlot) throws InvalidPassphraseException {
		if (paddedSlot.length != SLOT_SIZE) {
			throw new InvalidPassphraseException();
		}
		
		// Try to find length marker within reasonable range
		// Masterkey files are typically 300-2000 bytes
		// Search from smallest to largest likely size
		for (int possibleLength = 100; possibleLength <= SLOT_SIZE - 4; possibleLength++) {
			int lengthPos = possibleLength;
			
			// Read 4-byte length stored at this position (little-endian)
			int storedLength = (paddedSlot[lengthPos] & 0xFF) |
			                  ((paddedSlot[lengthPos + 1] & 0xFF) << 8) |
			                  ((paddedSlot[lengthPos + 2] & 0xFF) << 16) |
			                  ((paddedSlot[lengthPos + 3] & 0xFF) << 24);
			
			// Length marker is valid if:
			// 1. Stored length equals its position (self-referential)
			// 2. Length is positive and reasonable
			// 3. Length leaves room for the marker itself
			if (storedLength == possibleLength && 
			    storedLength > 50 && 
			    storedLength < SLOT_SIZE - 4) {
				// Found valid length marker - extract original data
				return Arrays.copyOfRange(paddedSlot, 0, storedLength);
			}
		}
		
		// No valid length marker found - this is random padding or wrong password
		throw new InvalidPassphraseException();
	}
	
	/**
	 * Find first empty slot (random padding).
	 * Returns -1 if all slots are occupied.
	 */
	private int findFirstEmptySlot(byte[] fileData) {
		for (int i = 0; i < NUM_SLOTS; i++) {
			byte[] slotData = extractSlot(fileData, i);
			try {
				unpadSlot(slotData);
				// Successfully unpadded - slot is occupied
			} catch (InvalidPassphraseException e) {
				// Failed to unpad - this is an empty slot
				return i;
			}
		}
		return -1; // All slots occupied
	}
	
	/**
	 * Count how many slots are occupied (not random padding).
	 * Used to prevent removing the last keyslot.
	 */
	private int countOccupiedSlots(byte[] fileData) {
		int count = 0;
		for (int i = 0; i < NUM_SLOTS; i++) {
			byte[] slotData = extractSlot(fileData, i);
			try {
				unpadSlot(slotData);
				count++;
			} catch (InvalidPassphraseException e) {
				// Empty slot
			}
		}
		return count;
	}
}
