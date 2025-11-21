package org.cryptomator.common.vaults;

import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper for lazy loading and on-demand reloading of the vault configuration.
 */
public class VaultConfigCache {

	private final VaultSettings settings;
	private final AtomicReference<VaultConfig.UnverifiedVaultConfig> config;

	public VaultConfigCache(VaultSettings settings) {
		this.settings = settings;
		this.config = new AtomicReference<>(null);
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
	 * Attempts to read the vault config file and parse it without verifying its integrity.
	 *
	 * @throws VaultConfigLoadException if the read file cannot be properly parsed
	 * @throws IOException if reading the file fails
	 */
	static VaultConfig.UnverifiedVaultConfig readConfigFromStorage(Path vaultPath) throws IOException {
		Path configPath = vaultPath.resolve(Constants.VAULTCONFIG_FILENAME);
		String token = Files.readString(configPath, StandardCharsets.US_ASCII);
		return VaultConfig.decode(token);
	}

}
