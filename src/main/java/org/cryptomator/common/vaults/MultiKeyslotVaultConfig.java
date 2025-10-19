package org.cryptomator.common.vaults;

import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
 * <p>This class implements a file format that stores multiple vault configurations
 * in a single file. Each configuration is a JWT token signed with a different masterkey.
 * This eliminates the need for separate vault.bak files, making hidden vaults
 * cryptographically undetectable.
 * 
 * <h2>File Format:</h2>
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │ Magic: "VCFG" (4 bytes)                 │
 * ├─────────────────────────────────────────┤
 * │ Version: 1 (4 bytes)                    │
 * ├─────────────────────────────────────────┤
 * │ Config Count: N (4 bytes)               │
 * ├─────────────────────────────────────────┤
 * │ Config 1 Size: X bytes (4 bytes)        │
 * │ Config 1 Data: (X bytes) - JWT token    │
 * ├─────────────────────────────────────────┤
 * │ Config 2 Size: Y bytes (4 bytes)        │
 * │ Config 2 Data: (Y bytes) - JWT token    │
 * ├─────────────────────────────────────────┤
 * │ ... (additional configs)                 │
 * └─────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Security Properties:</h2>
 * <ul>
 *   <li>Each config is signed with a different masterkey</li>
 *   <li>Cannot verify a config without the correct masterkey</li>
 *   <li>All configs appear as random base64 strings</li>
 *   <li>No way to detect number of vaults without trying passwords</li>
 *   <li>TRUE plausible deniability - no file presence reveals hidden vaults</li>
 * </ul>
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
	
	private static final byte[] MAGIC = "VCFG".getBytes(StandardCharsets.US_ASCII);
	private static final int VERSION = 1;
	private static final int HEADER_SIZE = 12; // magic(4) + version(4) + count(4)
	
	/**
	 * Check if a file is a multi-keyslot vault config file.
	 * 
	 * @param path Path to vault config file
	 * @return true if file has multi-keyslot format
	 * @throws IOException on I/O errors
	 */
	public boolean isMultiKeyslotFile(Path path) throws IOException {
		if (!Files.exists(path) || Files.size(path) < HEADER_SIZE) {
			return false;
		}
		
		byte[] header = new byte[4];
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			ByteBuffer buffer = ByteBuffer.wrap(header);
			channel.read(buffer);
		}
		
		return java.util.Arrays.equals(header, MAGIC);
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
		LOG.debug("Found {} config slot(s) in {}", configTokens.size(), path.getFileName());
		
		// Try each config slot with the provided masterkey
		for (int i = 0; i < configTokens.size(); i++) {
			try {
				VaultConfig.UnverifiedVaultConfig config = VaultConfig.decode(configTokens.get(i));
				
				// Try to verify with this masterkey
				// This will throw VaultConfigLoadException if masterkey doesn't match
				config.verify(masterkey, config.allegedVaultVersion());
				
				// Success! This config matches this masterkey
				LOG.info("Masterkey matched config slot {} of {}", i + 1, configTokens.size());
				return config;
				
			} catch (VaultConfigLoadException e) {
				// This config doesn't match this masterkey, try next slot
				LOG.trace("Masterkey didn't match config slot {}", i + 1);
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
		LOG.info("Created multi-keyslot vault config with 1 slot at {}", path.getFileName());
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
			LOG.info("Converting legacy vault.cryptomator to multi-keyslot format");
			String legacyConfig = Files.readString(path, StandardCharsets.US_ASCII);
			existingConfigs = new ArrayList<>(List.of(legacyConfig));
		}
		
		// Add new config
		List<String> allConfigs = new ArrayList<>(existingConfigs);
		allConfigs.add(newConfigToken);
		
		// Write back atomically
		Path tempFile = Files.createTempFile(path.getParent(), ".vault-config-", ".tmp");
		try {
			writeConfigSlots(tempFile, allConfigs);
			Files.move(tempFile, path, 
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE);
			LOG.info("Added config slot to {} (now {} slots)", path.getFileName(), allConfigs.size());
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
			LOG.warn("Masterkey doesn't match any config slot");
			return false;
		}
		
		// Remove the config
		List<String> newConfigs = new ArrayList<>(configs);
		newConfigs.remove(configToRemove);
		
		if (newConfigs.size() == 1) {
			// Convert back to legacy single-config format
			Files.writeString(path, newConfigs.get(0), StandardCharsets.US_ASCII);
			LOG.info("Removed config slot, converted back to single-config format");
		} else {
			writeConfigSlots(path, newConfigs);
			LOG.info("Removed config slot {} (now {} slots remaining)", configToRemove + 1, newConfigs.size());
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
	 * @return Number of config slots (1 for legacy files)
	 * @throws IOException on I/O errors
	 */
	public int getConfigSlotCount(Path path) throws IOException {
		if (!isMultiKeyslotFile(path)) {
			return 1; // Legacy single-config file
		}
		return readConfigSlots(path).size();
	}
	
	/**
	 * Read all config slots from the file.
	 * 
	 * @param path Path to multi-keyslot vault config file
	 * @return List of config JWT tokens
	 * @throws IOException on I/O errors
	 */
	private List<String> readConfigSlots(Path path) throws IOException {
		List<String> configs = new ArrayList<>();
		
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			// Read header
			ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
			channel.read(header);
			header.flip();
			
			byte[] magic = new byte[4];
			header.get(magic);
			int version = header.getInt();
			int count = header.getInt();
			
			if (!java.util.Arrays.equals(magic, MAGIC)) {
				throw new IOException("Invalid multi-keyslot vault config: bad magic bytes");
			}
			if (version != VERSION) {
				throw new IOException("Unsupported multi-keyslot vault config version: " + version);
			}
			
			// Read each config slot
			for (int i = 0; i < count; i++) {
				// Read size
				ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				channel.read(sizeBuffer);
				sizeBuffer.flip();
				int configSize = sizeBuffer.getInt();
				
				// Read config data
				ByteBuffer configBuffer = ByteBuffer.allocate(configSize);
				channel.read(configBuffer);
				configBuffer.flip();
				
				byte[] configData = new byte[configSize];
				configBuffer.get(configData);
				configs.add(new String(configData, StandardCharsets.US_ASCII));
			}
		}
		
		return configs;
	}
	
	/**
	 * Write config slots to file.
	 * 
	 * @param path Path where file will be written
	 * @param configs List of config JWT tokens to write
	 * @throws IOException on I/O errors
	 */
	private void writeConfigSlots(Path path, List<String> configs) throws IOException {
		try (FileChannel channel = FileChannel.open(path,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			
			// Write header
			ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
			header.put(MAGIC);
			header.putInt(VERSION);
			header.putInt(configs.size());
			header.flip();
			channel.write(header);
			
			// Write each config slot
			for (String config : configs) {
				byte[] configBytes = config.getBytes(StandardCharsets.US_ASCII);
				
				// Write size
				ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
				sizeBuffer.putInt(configBytes.length);
				sizeBuffer.flip();
				channel.write(sizeBuffer);
				
				// Write data
				ByteBuffer configBuffer = ByteBuffer.wrap(configBytes);
				channel.write(configBuffer);
			}
		}
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
