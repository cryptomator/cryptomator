/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.DirStructure;
import org.cryptomator.cryptofs.migration.Migrators;
import org.cryptomator.integrations.mount.MountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;
import static org.cryptomator.common.vaults.VaultState.Value.ERROR;
import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;

@Singleton
public class VaultListManager {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListManager.class);

	private final AutoLocker autoLocker;
	private final List<MountService> mountServices;
	private final VaultComponent.Factory vaultComponentFactory;
	private final ObservableList<Vault> vaultList;
	private final String defaultVaultName;

	@Inject
	public VaultListManager(ObservableList<Vault> vaultList, //
							AutoLocker autoLocker, //
							List<MountService> mountServices,
							VaultComponent.Factory vaultComponentFactory,
							ResourceBundle resourceBundle,
							Settings settings) {
		this.vaultList = vaultList;
		this.autoLocker = autoLocker;
		this.mountServices = mountServices;
		this.vaultComponentFactory = vaultComponentFactory;
		this.defaultVaultName = resourceBundle.getString("defaults.vault.vaultName");

		addAll(settings.directories);
		vaultList.addListener(new VaultListChangeListener(settings.directories));
		autoLocker.init();
	}

	public Vault add(Path pathToVault) throws IOException {
		Path normalizedPathToVault = pathToVault.normalize().toAbsolutePath();
		if (CryptoFileSystemProvider.checkDirStructureForVault(normalizedPathToVault, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME) == DirStructure.UNRELATED) {
			throw new NoSuchFileException(normalizedPathToVault.toString(), null, "Not a vault directory");
		}

		return get(normalizedPathToVault) //
				.orElseGet(() -> {
					Vault newVault = create(newVaultSettings(normalizedPathToVault));
					try {
						setVaultScheme(newVault);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					vaultList.add(newVault);
					return newVault;
				});
	}

	private void setVaultScheme(Vault vault) throws IOException {
		try {
			var lastKnownKeyLoader = vault.getVaultSettings().lastKnownKeyLoader;
			if (Objects.isNull(lastKnownKeyLoader.get())) {
				var vaultConfig = vault.getVaultConfigCache().get();
				var keyIdScheme = vaultConfig.getKeyId().getScheme();
				lastKnownKeyLoader.set(keyIdScheme);
			}
		} catch (NoSuchFileException e) {
			vault.stateProperty().set(VaultState.Value.ERROR);
			LOG.error("Configuration file missing or corrupted.", e);
		} catch (IOException e) {
			vault.stateProperty().set(VaultState.Value.ERROR);
			LOG.error("Unexpected IO exception while setting vault scheme.", e);
			throw new IOException("Configuration file missing or corrupted.", e);
		}
	}

	private VaultSettings newVaultSettings(Path path) {
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path.set(path);
		if (path.getFileName() != null) {
			vaultSettings.displayName.set(path.getFileName().toString());
		} else {
			vaultSettings.displayName.set(defaultVaultName);
		}

		//due to https://github.com/cryptomator/cryptomator/issues/2880#issuecomment-1680313498
		var nameOfWinfspLocalMounter = "org.cryptomator.frontend.fuse.mount.WinFspMountProvider";
		if (SystemUtils.IS_OS_WINDOWS //
				&& vaultSettings.path.get().toString().contains("Dropbox") //
				&& mountServices.stream().anyMatch(s -> s.getClass().getName().equals(nameOfWinfspLocalMounter))) {
			vaultSettings.mountService.setValue(nameOfWinfspLocalMounter);
		}

		return vaultSettings;
	}

	private void addAll(Collection<VaultSettings> vaultSettings) {
		Collection<Vault> vaults = vaultSettings.stream().map(this::create).toList();
		vaultList.addAll(vaults);
		for (Vault vault : vaults) {
			try {
				setVaultScheme(vault);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private Optional<Vault> get(Path vaultPath) {
		assert vaultPath.isAbsolute();
		assert vaultPath.normalize().equals(vaultPath);
		return vaultList.stream() //
				.filter(v -> vaultPath.equals(v.getPath())) //
				.findAny();
	}

	private Vault create(VaultSettings vaultSettings) {
		var wrapper = new VaultConfigCache(vaultSettings);
		try {
			var vaultState = determineVaultState(vaultSettings.path.get());
			if (vaultState == LOCKED) { //for legacy reasons: pre v8 vault do not have a config, but they are in the NEEDS_MIGRATION state
				wrapper.reloadConfig();
			}
			return vaultComponentFactory.create(vaultSettings, wrapper, vaultState, null).vault();
		} catch (IOException e) {
			LOG.warn("Failed to determine vault state for " + vaultSettings.path.get(), e);
			return vaultComponentFactory.create(vaultSettings, wrapper, ERROR, e).vault();
		}
	}

	public static VaultState.Value redetermineVaultState(Vault vault) {
		VaultState state = vault.stateProperty();
		VaultState.Value previousState = state.getValue();
		return switch (previousState) {
			case LOCKED, NEEDS_MIGRATION, MISSING -> {
				try {
					var determinedState = determineVaultState(vault.getPath());
					if (determinedState == LOCKED) {
						vault.getVaultConfigCache().reloadConfig();
					}
					state.set(determinedState);
					yield determinedState;
				} catch (IOException e) {
					LOG.warn("Failed to determine vault state for " + vault.getPath(), e);
					state.set(ERROR);
					vault.setLastKnownException(e);
					yield ERROR;
				}
			}
			case ERROR, UNLOCKED, PROCESSING -> previousState;
		};
	}

	private static VaultState.Value determineVaultState(Path pathToVault) throws IOException {
		if (!Files.exists(pathToVault)) {
			return VaultState.Value.MISSING;
		}
		return switch (CryptoFileSystemProvider.checkDirStructureForVault(pathToVault, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME)) {
			case VAULT -> VaultState.Value.LOCKED;
			case UNRELATED -> VaultState.Value.MISSING;
			case MAYBE_LEGACY -> Migrators.get().needsMigration(pathToVault, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME) ? //
					VaultState.Value.NEEDS_MIGRATION //
					: VaultState.Value.MISSING;
		};
	}

}
