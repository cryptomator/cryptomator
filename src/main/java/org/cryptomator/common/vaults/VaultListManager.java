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
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;
import static org.cryptomator.common.vaults.VaultState.Value.ERROR;
import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;
import static org.cryptomator.common.vaults.VaultState.Value.MASTERKEY_MISSING;
import static org.cryptomator.common.vaults.VaultState.Value.VAULT_CONFIG_MISSING;

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
							List<MountService> mountServices, //
							VaultComponent.Factory vaultComponentFactory, //
							ResourceBundle resourceBundle, //
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
					vaultList.add(newVault);
					return newVault;
				});
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
		vaults.forEach(vault -> {
			VaultState.Value newState = redetermineVaultState(vault);
			LOG.info("New state for vault at {}: {}", vault.getPath(), newState);
		});
		vaultList.addAll(vaults);
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
			if (Objects.isNull(vaultSettings.lastKnownKeyLoader.get())) {
				var keyIdScheme = wrapper.get().getKeyId().getScheme();
				vaultSettings.lastKnownKeyLoader.set(keyIdScheme);
			}
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
			case LOCKED, NEEDS_MIGRATION, MISSING, VAULT_CONFIG_MISSING, MASTERKEY_MISSING -> {
				try {
					var determinedState = determineVaultState(vault.getPath());
					if(determinedState == MASTERKEY_MISSING){
						var vaultScheme = vault.getVaultConfigCache().getUnchecked().getKeyId().getScheme();
						if((vaultScheme.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTP) || vaultScheme.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTPS))){
							determinedState = LOCKED;
						}
					}
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
		Path pathToVaultConfig = Path.of(pathToVault.toString(),"vault.cryptomator");
		Path pathToMasterkey = Path.of(pathToVault.toString(),"masterkey.cryptomator");
		if (!Files.exists(pathToVault)) {
			return VaultState.Value.MISSING;
		}
		else if (!Files.exists(pathToVaultConfig)) {
			try (Stream<Path> files = Files.list(pathToVaultConfig.getParent())) {
				Path backupFile = files.filter(file -> {
					String fileName = file.getFileName().toString();
					return fileName.startsWith("vault.cryptomator") && fileName.endsWith(".bkup");
				}).findFirst().orElse(null);

				if (backupFile != null) {
					try {
						Files.copy(backupFile, pathToVaultConfig, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						LOG.error("error",e);
						return VAULT_CONFIG_MISSING;
					}
				} else {
					return VAULT_CONFIG_MISSING;
				}
			} catch (IOException e) {
				LOG.error("error",e);
				return VAULT_CONFIG_MISSING;
			}

		}
		else if (!Files.exists(pathToMasterkey)) {
			try (Stream<Path> files = Files.list(pathToMasterkey.getParent())) {
				Path backupFile = files.filter(file -> {
					String fileName = file.getFileName().toString();
					return fileName.startsWith("masterkey.cryptomator") && fileName.endsWith(".bkup");
				}).findFirst().orElse(null);

				if (backupFile != null) {
					try {
						Files.copy(backupFile, pathToMasterkey, StandardCopyOption.REPLACE_EXISTING);
						return MASTERKEY_MISSING;
					} catch (IOException e) {
						LOG.error("error",e);
						return MASTERKEY_MISSING;
					}
				} else {
					return MASTERKEY_MISSING;
				}
			} catch (IOException e) {
				LOG.error("error",e);
				return MASTERKEY_MISSING;
			}
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
