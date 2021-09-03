package org.cryptomator.common.vaults;

import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Wrapper for lazy loading and on-demand reloading of the vault configuration.
 */
public class VaultConfigWrapper {

	private final VaultSettings settings;
	private final ObjectProperty<VaultConfig.UnverifiedVaultConfig> config;

	VaultConfigWrapper(VaultSettings settings) {
		this.settings = settings;
		this.config = new SimpleObjectProperty<>();
	}

	void reloadConfig() throws IOException {
		config.set(readConfigFromStorage(this.settings.path().get()));
	}

	VaultConfig.UnverifiedVaultConfig getConfig() throws IOException {
		if (Objects.isNull(config.get())) {
			reloadConfig();
		}
		return config.get();
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
