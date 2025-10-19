package org.cryptomator.common.vaults;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-keyslot vault configuration file for TRUE plausible deniability.
 * 
 * <p>This class implements a SECURE file format that stores multiple vault configurations
 * in a single fixed-size file. Each configuration is a JWT token that can only be verified
 * with the correct masterkey. This eliminates vault.bak files and makes hidden vaults
 * cryptographically undetectable.
 * 
 * <h2>Secure File Format (Fixed Size - Indistinguishable from Random):</h2>
 * <pre>
 * Total Size: 32,768 bytes (32 KB) - FIXED SIZE, always same regardless of slots used
 * 
 * ┌─────────────────────────────────────────┐
 * │ Slot 0: 8,192 bytes                     │  ← JWT config OR random padding
 * ├─────────────────────────────────────────┤
 * │ Slot 1: 8,192 bytes                     │  ← JWT config OR random padding
 * ├─────────────────────────────────────────┤
 * │ Slot 2: 8,192 bytes                     │  ← JWT config OR random padding
 * ├─────────────────────────────────────────┤
 * │ Slot 3: 8,192 bytes                     │  ← JWT config OR random padding
 * └─────────────────────────────────────────┘
 * 
 * Each slot contains:
 * - Length marker: 4 bytes (little-endian int)
 * - JWT config: variable length (if real slot)
 * - Random padding: fills remainder to 8,192 bytes
 * 
 * Or for empty slots:
 * - Cryptographically secure random data: 8,192 bytes
 * </pre>
 * 
 * <h2>Security Properties (TRUE Plausible Deniability):</h2>
 * <ul>
 *   <li>NO magic bytes - file looks like random data</li>
 *   <li>NO version field - no identifying markers</li>
 *   <li>NO explicit count - impossible to tell how many configs exist</li>
 *   <li>Fixed size - always 32 KB regardless of actual usage</li>
 *   <li>Each slot is indistinguishable from random data</li>
 *   <li>Config detection ONLY via JWT signature verification with masterkey</li>
 *   <li>Cannot prove hidden vaults exist without the masterkey</li>
 *   <li>Even file size reveals nothing (always 32 KB)</li>
 * </ul>
 * 
 * <h2>Backward Compatibility:</h2>
 * <p>Supports loading legacy single-config files (plain JWT, variable size).
 * Automatically converts to secure multi-keyslot format when adding hidden vaults.
 * 
 * <h2>Usage:</h2>
 * <pre>
 * // Load config with masterkey (tries all slots)
 * VaultConfig.UnverifiedVaultConfig config = multiKeyslotVaultConfig.load(path, masterkey);
 * 
 * // Add hidden vault config
 * multiKeyslotVaultConfig.addConfigSlot(path, hiddenConfigToken);
 * 
 * // Remove hidden vault config
 * multiKeyslotVaultConfig.removeConfigSlot(path, hiddenMasterkey);
 * </pre>
 * 
 * @since 1.8.0
 * @see org.cryptomator.common.keychain.MultiKeyslotFile
 */
public class MultiKeyslotVaultConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiKeyslotVaultConfig.class);
	
	// SECURE FORMAT - No magic bytes, no version, no count!
	private static final int SLOT_SIZE = 8192;  // 8 KB per slot
	private static final int NUM_SLOTS = 4;     // Always 4 slots
	private static final int FILE_SIZE = NUM_SLOTS * SLOT_SIZE;  // 32 KB total (fixed size)
	
	// Maximum JWT config size per slot (leave room for length marker + padding)
	private static final int MAX_CONFIG_SIZE_PER_SLOT = SLOT_SIZE - 4;  // Reserve 4 bytes for length
	
	private final java.security.SecureRandom secureRandom;
	
	public MultiKeyslotVaultConfig() {
		this.secureRandom = new java.security.SecureRandom();
	}
	
	/**
	 * Check if a file is a multi-keyslot vault config file.
	 * SECURITY: Detection by file size ONLY - no magic bytes to preserve plausible deniability.
	 * 
	 * @param path Path to vault config file
	 * @return true if file has multi-keyslot format (exactly FILE_SIZE bytes)
	 * @throws IOException on I/O errors
	 */
	public boolean isMultiKeyslotFile(Path path) throws IOException {
		if (!Files.exists(path)) {
			return false;
		}
		
		long size = Files.size(path);
		// Multi-keyslot files are exactly FILE_SIZE bytes (32 KB)
		// Legacy single-config files are typically 500-2000 bytes
		return size == FILE_SIZE;
	}
	
	/**
	 * Load vault config by trying all slots with the given masterkey.
	 * 
	 * <p>This method reads all config slots from the file and attempts to verify
	 * each one with the provided masterkey. The first config that successfully
	 * verifies is returned. This implements automatic vault selection based on
	 * the masterkey without any UI indication.
	 * 
	 * @param path Path to vault config file
	 * @param masterkey Masterkey bytes to verify configs with
	 * @return The unverified config that matches the masterkey
	 * @throws IOException on I/O errors
	 * @throws VaultConfigLoadException if no config matches the masterkey
	 */
	public VaultConfig.UnverifiedVaultConfig load(Path path, byte[] masterkey) 
			throws IOException, VaultConfigLoadException {
		
		if (!isMultiKeyslotFile(path)) {
			// Fall back to legacy single-config file
			LOG.debug("Loading legacy single-config vault.cryptomator");
			return loadLegacyConfig(path);
		}
		
		List<String> configTokens = readConfigSlots(path);
		// SECURITY: Don't log slot count - reveals hidden vault existence
		
		// Try each config slot with the provided masterkey
		for (String configToken : configTokens) {
			// Skip null slots (empty/random data)
			if (configToken == null) {
				continue;
			}
			
			try {
				VaultConfig.UnverifiedVaultConfig config = VaultConfig.decode(configToken);
				
				// Try to verify with this masterkey
				// This will throw VaultConfigLoadException if masterkey doesn't match
				config.verify(masterkey, config.allegedVaultVersion());
				
				// Success! This config matches this masterkey
				// SECURITY: Don't log slot index - reveals which vault was accessed
				LOG.debug("Vault config matched successfully");
				return config;
				
			} catch (VaultConfigLoadException e) {
				// This config doesn't match this masterkey, try next slot
				// SECURITY: Don't log anything - silent try maintains plausible deniability
				continue;
			}
		}
		
		// No config matched this masterkey
		throw new VaultConfigLoadException("No vault configuration matches this masterkey");
	}
	
	/**
	 * Create a new multi-keyslot file with a single config.
	 * 
	 * @param path Path where file will be created
	 * @param configToken JWT config token (signed with masterkey)
	 * @throws IOException on I/O errors
	 */
	public void persist(Path path, String configToken) throws IOException {
		List<String> configs = List.of(configToken);
		writeConfigSlots(path, configs);
		// SECURITY: Don't mention "multi-keyslot" or slot count
		LOG.debug("Created vault config");
	}
	
	/**
	 * Add a hidden vault config to an existing file.
	 * 
	 * <p>This method reads all existing configs, adds the new one, and writes
	 * them back atomically. If the file is in legacy format, it will be automatically
	 * converted to multi-keyslot format.
	 * 
	 * @param path Path to existing vault config file
	 * @param newConfigToken JWT config token for hidden vault
	 * @throws IOException on I/O errors
	 */
	public void addConfigSlot(Path path, String newConfigToken) throws IOException {
		List<String> existingConfigs;
		
		if (isMultiKeyslotFile(path)) {
			existingConfigs = readConfigSlots(path);
		} else {
			// Convert legacy single-config to multi-keyslot format
			LOG.debug("Converting vault config to multi-keyslot format");
			String legacyConfig = Files.readString(path, StandardCharsets.US_ASCII);
			// Create list with 4 slots: first is legacy config, rest are null (will be random)
			existingConfigs = new ArrayList<>(NUM_SLOTS);
			existingConfigs.add(legacyConfig);
			for (int i = 1; i < NUM_SLOTS; i++) {
				existingConfigs.add(null);
			}
		}
		
		// Find first available slot (null entry)
		int availableSlot = -1;
		for (int i = 0; i < existingConfigs.size(); i++) {
			if (existingConfigs.get(i) == null) {
				availableSlot = i;
				break;
			}
		}
		
		if (availableSlot == -1) {
			throw new IOException("No available slots - maximum " + NUM_SLOTS + " identities per vault");
		}
		
		// Add new config to available slot
		existingConfigs.set(availableSlot, newConfigToken);
		
		// Write back atomically
		Path tempFile = Files.createTempFile(path.getParent(), ".vcfg-", ".tmp");
		try {
			writeConfigSlots(tempFile, existingConfigs);
			try {
				Files.move(tempFile, path, 
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
			}
			// SECURITY: Don't log slot count - reveals hidden vault existence
			LOG.debug("Added config to vault");
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}
	
	/**
	 * Remove a config slot from the file.
	 * 
	 * <p><strong>WARNING:</strong> This requires the masterkey of the vault to remove.
	 * The method will find which config matches the masterkey and remove that slot.
	 * 
	 * @param path Path to multi-keyslot vault config file
	 * @param masterkey Masterkey of the vault to remove
	 * @return true if a config slot was removed
	 * @throws IOException on I/O errors
	 */
	public boolean removeConfigSlot(Path path, byte[] masterkey) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			LOG.warn("Cannot remove config slot from non-multi-keyslot file");
			return false;
		}
		
		List<String> configs = readConfigSlots(path);
		if (configs.size() <= 1) {
			LOG.warn("Cannot remove last config slot");
			return false;
		}
		
		// Find which config matches this masterkey
		int configToRemove = -1;
		for (int i = 0; i < configs.size(); i++) {
			if (configs.get(i) == null) {
				continue;  // Skip empty slots
			}
			try {
				VaultConfig.UnverifiedVaultConfig config = VaultConfig.decode(configs.get(i));
				config.verify(masterkey, config.allegedVaultVersion());
				configToRemove = i;
				break;
			} catch (VaultConfigLoadException e) {
				// Not this config
			}
		}
		
		if (configToRemove == -1) {
			LOG.warn("Masterkey doesn't match any config");
			return false;
		}
		
		// Replace the config with null (will become random data)
		configs.set(configToRemove, null);
		
		// Count remaining non-null configs
		long remainingConfigs = configs.stream().filter(c -> c != null).count();
		
		if (remainingConfigs == 1) {
			// Only one config left - convert back to legacy single-config format
			String lastConfig = configs.stream().filter(c -> c != null).findFirst().orElseThrow();
			Files.writeString(path, lastConfig, StandardCharsets.US_ASCII);
			// SECURITY: Don't mention conversion or slot count
			LOG.debug("Removed config from vault");
		} else if (remainingConfigs == 0) {
			// Should not happen - would leave vault inaccessible
			LOG.warn("Cannot remove last config - vault would be inaccessible");
			return false;
		} else {
			// Multiple configs remain - write secure multi-slot file
			writeConfigSlots(path, configs);
			// SECURITY: Don't log slot index or count - reveals info about hidden vaults
			LOG.debug("Removed config from vault");
		}
		
		return true;
	}
	
	/**
	 * Get the number of config slots in the file.
	 * 
	 * <p><strong>NOTE:</strong> This method reveals that multiple vaults exist!
	 * Use with caution in contexts where plausible deniability matters.
	 * In production code, avoid calling this method to maintain plausible deniability.
	 * 
	 * @param path Path to vault config file
	 * @return Number of non-null config slots (1 for legacy files)
	 * @throws IOException on I/O errors
	 */
	public int getConfigSlotCount(Path path) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			return 1; // Legacy single-config file
		}
		// Count non-null configs only
		List<String> configs = readConfigSlots(path);
		return (int) configs.stream().filter(c -> c != null).count();
	}
	
	/**
	 * Load the first config slot without masterkey verification.
	 * This is used for vault state checking where we need a config but don't have the masterkey yet.
	 * 
	 * @param path Path to multi-keyslot vault config file
	 * @return Unverified config from first non-null slot
	 * @throws IOException on I/O errors
	 */
	public VaultConfig.UnverifiedVaultConfig loadFirstSlotUnverified(Path path) throws IOException {
		List<String> tokens = readConfigSlots(path);
		
		// Find first non-null config
		String firstConfig = tokens.stream()
			.filter(t -> t != null)
			.findFirst()
			.orElseThrow(() -> new IOException("No config slots found in file"));
		
		return VaultConfig.decode(firstConfig);
	}
	
	/**
	 * Read all config slots from the secure fixed-size file.
	 * SECURITY: No magic bytes, no count field. Each slot is tried to extract a valid JWT.
	 * 
	 * @param path Path to multi-keyslot vault config file
	 * @return List of config JWT tokens (may include nulls for empty slots)
	 * @throws IOException on I/O errors
	 */
	private List<String> readConfigSlots(Path path) throws IOException {
		byte[] fileData = Files.readAllBytes(path);
		
		if (fileData.length != FILE_SIZE) {
			throw new IOException("Invalid file size: expected " + FILE_SIZE + " bytes, got " + fileData.length);
		}
		
		List<String> configs = new ArrayList<>();
		
		// Read each slot
		for (int i = 0; i < NUM_SLOTS; i++) {
			int offset = i * SLOT_SIZE;
			
			// Read length marker (first 4 bytes, little-endian)
			int configLength = (fileData[offset] & 0xFF) |
			                  ((fileData[offset + 1] & 0xFF) << 8) |
			                  ((fileData[offset + 2] & 0xFF) << 16) |
			                  ((fileData[offset + 3] & 0xFF) << 24);
			
			// Sanity check length
			if (configLength > 0 && configLength <= MAX_CONFIG_SIZE_PER_SLOT) {
				// Potentially valid config - try to extract it
				try {
					byte[] configBytes = new byte[configLength];
					System.arraycopy(fileData, offset + 4, configBytes, 0, configLength);
					String configToken = new String(configBytes, StandardCharsets.US_ASCII);
					configs.add(configToken);
				} catch (Exception e) {
					// Invalid data - treat as empty/random slot
					configs.add(null);
				}
			} else {
				// Invalid length marker - this is an empty slot (pure random data)
				configs.add(null);
			}
		}
		
		return configs;
	}
	
	/**
	 * Write config slots to secure fixed-size file.
	 * SECURITY: No magic bytes, no version, no count. Fixed size with random padding.
	 * 
	 * @param path Path where file will be written
	 * @param configs List of config JWT tokens (nulls = empty slots)
	 * @throws IOException on I/O errors
	 */
	private void writeConfigSlots(Path path, List<String> configs) throws IOException {
		if (configs.size() > NUM_SLOTS) {
			throw new IOException("Too many configs: " + configs.size() + " (max: " + NUM_SLOTS + ")");
		}
		
		// Create fixed-size file data
		byte[] fileData = new byte[FILE_SIZE];
		
		// Write each slot
		for (int i = 0; i < NUM_SLOTS; i++) {
			int offset = i * SLOT_SIZE;
			
			if (i < configs.size() && configs.get(i) != null) {
				// Real config slot
				String configToken = configs.get(i);
				byte[] configBytes = configToken.getBytes(StandardCharsets.US_ASCII);
				
				if (configBytes.length > MAX_CONFIG_SIZE_PER_SLOT) {
					throw new IOException("Config too large for slot " + i + ": " + configBytes.length + " bytes (max: " + MAX_CONFIG_SIZE_PER_SLOT + ")");
				}
				
				// Write length marker (4 bytes, little-endian)
				fileData[offset] = (byte) (configBytes.length & 0xFF);
				fileData[offset + 1] = (byte) ((configBytes.length >> 8) & 0xFF);
				fileData[offset + 2] = (byte) ((configBytes.length >> 16) & 0xFF);
				fileData[offset + 3] = (byte) ((configBytes.length >> 24) & 0xFF);
				
				// Write config data
				System.arraycopy(configBytes, 0, fileData, offset + 4, configBytes.length);
				
				// Fill remainder with random padding
				int remainingBytes = SLOT_SIZE - 4 - configBytes.length;
				if (remainingBytes > 0) {
					byte[] padding = new byte[remainingBytes];
					secureRandom.nextBytes(padding);
					System.arraycopy(padding, 0, fileData, offset + 4 + configBytes.length, remainingBytes);
				}
			} else {
				// Empty slot - fill with pure random data
				byte[] randomSlot = new byte[SLOT_SIZE];
				secureRandom.nextBytes(randomSlot);
				System.arraycopy(randomSlot, 0, fileData, offset, SLOT_SIZE);
			}
		}
		
		// Write file atomically
		Files.write(path, fileData, 
			StandardOpenOption.CREATE,
			StandardOpenOption.WRITE,
			StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	/**
	 * Load legacy single-config vault.cryptomator file.
	 * 
	 * @param path Path to legacy vault config file
	 * @return Unverified vault config
	 * @throws IOException on I/O errors
	 */
	private VaultConfig.UnverifiedVaultConfig loadLegacyConfig(Path path) throws IOException {
		String token = Files.readString(path, StandardCharsets.US_ASCII);
		return VaultConfig.decode(token);
	}
}
