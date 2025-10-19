package org.cryptomator.common.vaults;

import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper for lazy loading and on-demand reloading of the vault configuration.
 * Supports both legacy single-config and multi-keyslot vault configurations.
 */
public class VaultConfigCache {

	private final VaultSettings settings;
	private final AtomicReference<VaultConfig.UnverifiedVaultConfig> config;
	private final MultiKeyslotVaultConfig multiKeyslotVaultConfig;

	// Constructor for direct instantiation (backward compatibility)
	public VaultConfigCache(VaultSettings settings) {
		this(settings, new MultiKeyslotVaultConfig());
	}

	// Constructor for dependency injection
	@Inject
	public VaultConfigCache(VaultSettings settings, MultiKeyslotVaultConfig multiKeyslotVaultConfig) {
		this.settings = settings;
		this.config = new AtomicReference<>(null);
		this.multiKeyslotVaultConfig = multiKeyslotVaultConfig;
	}

	void reloadConfig() throws IOException {
		try {
			config.set(readConfigFromStorage(this.settings.path.get()));
		} catch (IOException e) {
			config.set(null);
			throw e;
		}
	}

	public VaultConfig.UnverifiedVaultConfig get() throws IOException {
		if (config.get() == null) {
			reloadConfig();
		}
		return config.get();
	}

	public VaultConfig.UnverifiedVaultConfig getUnchecked() {
		try {
			return get();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * Load vault config using the provided masterkey.
	 * This method supports multi-keyslot vault configs, automatically selecting
	 * the config that matches the masterkey.
	 * 
	 * @param masterkey Masterkey bytes to verify config with
	 * @return Unverified vault config that matches the masterkey
	 * @throws IOException on I/O errors
	 * @throws VaultConfigLoadException if no config matches the masterkey
	 */
	public VaultConfig.UnverifiedVaultConfig getWithMasterkey(byte[] masterkey) 
			throws IOException, VaultConfigLoadException {
		Path vaultPath = settings.path.get();
		Path configPath = vaultPath.resolve(Constants.VAULTCONFIG_FILENAME);
		return multiKeyslotVaultConfig.load(configPath, masterkey);
	}

	/**
	 * Attempts to read the vault config file and parse it without verifying its integrity.
	 * Handles both legacy single-config and multi-keyslot vault configs.
	 * For multi-keyslot files, returns the first config (primary vault).
	 *
	 * @throws VaultConfigLoadException if the read file cannot be properly parsed
	 * @throws IOException if reading the file fails
	 */
	static VaultConfig.UnverifiedVaultConfig readConfigFromStorage(Path vaultPath) throws IOException {
		Path configPath = vaultPath.resolve(Constants.VAULTCONFIG_FILENAME);
		
		// Check if this is a multi-keyslot file
		MultiKeyslotVaultConfig multiKeyslot = new MultiKeyslotVaultConfig();
		if (multiKeyslot.isMultiKeyslotFile(configPath)) {
			// For multi-keyslot files, we can't determine which config without a masterkey
			// So we return the first config (primary vault) for vault state checking
			try {
				// Read first config slot manually
				byte[] fileData = Files.readAllBytes(configPath);
				if (fileData.length < 16) { // Header is 12 bytes + at least 4 for size
					throw new IOException("Multi-keyslot file too small");
				}
				
				// Skip header (12 bytes: magic + version + count)
				int offset = 12;
				
				// Read first config size (4 bytes, big-endian)
				int configSize = ((fileData[offset] & 0xFF) << 24) |
								 ((fileData[offset + 1] & 0xFF) << 16) |
								 ((fileData[offset + 2] & 0xFF) << 8) |
								 (fileData[offset + 3] & 0xFF);
				offset += 4;
				
				// Read first config token
				String token = new String(fileData, offset, configSize, StandardCharsets.US_ASCII);
				return VaultConfig.decode(token);
			} catch (Exception e) {
				throw new IOException("Failed to read first config from multi-keyslot file", e);
			}
		} else {
			// Legacy single-config file
			String token = Files.readString(configPath, StandardCharsets.US_ASCII);
			return VaultConfig.decode(token);
		}
	}

}
