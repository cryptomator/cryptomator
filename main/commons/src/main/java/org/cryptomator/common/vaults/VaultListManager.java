/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.migration.Migrators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@Singleton
public class VaultListManager {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListManager.class);

	private final VaultComponent.Builder vaultComponentBuilder;
	private final ObservableList<Vault> vaultList;
	private final String defaultVaultName;

	@Inject
	public VaultListManager(VaultComponent.Builder vaultComponentBuilder, ResourceBundle resourceBundle, Settings settings) {
		this.vaultComponentBuilder = vaultComponentBuilder;
		this.defaultVaultName = resourceBundle.getString("defaults.vault.vaultName");
		this.vaultList = FXCollections.observableArrayList(Vault::observables);

		addAll(settings.getDirectories());
		vaultList.addListener(new VaultListChangeListener(settings.getDirectories()));
	}

	public ObservableList<Vault> getVaultList() {
		return vaultList;
	}

	public Vault add(Path pathToVault) throws NoSuchFileException {
		Path normalizedPathToVault = pathToVault.normalize().toAbsolutePath();
		if (!CryptoFileSystemProvider.containsVault(normalizedPathToVault, MASTERKEY_FILENAME)) {
			throw new NoSuchFileException(normalizedPathToVault.toString(), null, "Not a vault directory");
		}
		Optional<Vault> alreadyExistingVault = get(normalizedPathToVault);
		if (alreadyExistingVault.isPresent()) {
			return alreadyExistingVault.get();
		} else {
			Vault newVault = create(newVaultSettings(normalizedPathToVault));
			vaultList.add(newVault);
			return newVault;
		}
	}

	private VaultSettings newVaultSettings(Path path) {
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path().set(path);
		if (path.getFileName() != null) {
			vaultSettings.displayName().set(path.getFileName().toString());
		} else {
			vaultSettings.displayName().set(defaultVaultName);
		}
		return vaultSettings;
	}

	private void addAll(Collection<VaultSettings> vaultSettings) {
		Collection<Vault> vaults = vaultSettings.stream().map(this::create).toList();
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
		VaultComponent.Builder compBuilder = vaultComponentBuilder.vaultSettings(vaultSettings);
		try {
			VaultState.Value vaultState = determineVaultState(vaultSettings.path().get());
			compBuilder.initialVaultState(vaultState);
		} catch (IOException e) {
			LOG.warn("Failed to determine vault state for " + vaultSettings.path().get(), e);
			compBuilder.initialVaultState(VaultState.Value.ERROR);
			compBuilder.initialErrorCause(e);
		}
		return compBuilder.build().vault();
	}

	public static VaultState.Value redetermineVaultState(Vault vault) {
		VaultState state = vault.stateProperty();
		VaultState.Value previousState = state.getValue();
		return switch (previousState) {
			case LOCKED, NEEDS_MIGRATION, MISSING -> {
				try {
					VaultState.Value determinedState = determineVaultState(vault.getPath());
					state.set(determinedState);
					yield determinedState;
				} catch (IOException e) {
					LOG.warn("Failed to determine vault state for " + vault.getPath(), e);
					state.set(VaultState.Value.ERROR);
					vault.setLastKnownException(e);
					yield VaultState.Value.ERROR;
				}
			}
			case ERROR, UNLOCKED, PROCESSING -> previousState;
		};
	}

	private static VaultState.Value determineVaultState(Path pathToVault) throws IOException {
		if (!CryptoFileSystemProvider.containsVault(pathToVault, MASTERKEY_FILENAME)) {
			return VaultState.Value.MISSING;
		} else if (Migrators.get().needsMigration(pathToVault, MASTERKEY_FILENAME)) {
			return VaultState.Value.NEEDS_MIGRATION;
		} else {
			return VaultState.Value.LOCKED;
		}
	}

}
