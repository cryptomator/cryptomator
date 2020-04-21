/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.migration.Migrators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@Singleton
public class VaultListManager {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListManager.class);

	private final VaultComponent.Builder vaultComponentBuilder;
	private final ObservableList<Vault> vaultList;

	@Inject
	public VaultListManager(VaultComponent.Builder vaultComponentBuilder, Settings settings) {
		this.vaultComponentBuilder = vaultComponentBuilder;
		this.vaultList = FXCollections.observableArrayList(Vault::observables);

		addAll(settings.getDirectories());
		vaultList.addListener(new VaultListChangeListener(settings.getDirectories()));
	}

	public ObservableList<Vault> getVaultList() {
		return vaultList;
	}

	public Vault add(Path pathToVault) throws NoSuchFileException {
		if (!CryptoFileSystemProvider.containsVault(pathToVault, MASTERKEY_FILENAME)) {
			throw new NoSuchFileException(pathToVault.toString(), null, "Not a vault directory");
		}
		Optional<Vault> alreadyExistingVault = get(pathToVault);
		if (alreadyExistingVault.isPresent()) {
			return alreadyExistingVault.get();
		} else {
			VaultSettings vaultSettings = VaultSettings.withRandomId();
			vaultSettings.path().set(pathToVault);
			Vault newVault = create(vaultSettings);
			vaultList.add(newVault);
			return newVault;
		}
	}

	private void addAll(Collection<VaultSettings> vaultSettings) {
		Collection<Vault> vaults = vaultSettings.stream().map(this::create).collect(Collectors.toList());
		vaultList.addAll(vaults);
	}

	private Optional<Vault> get(Path vaultPath) {
		return vaultList.stream().filter(v -> {
			try {
				return Files.isSameFile(vaultPath, v.getPath());
			} catch (IOException e) {
				return false;
			}
		}).findAny();
	}

	private Vault create(VaultSettings vaultSettings) {
		VaultComponent.Builder compBuilder = vaultComponentBuilder.vaultSettings(vaultSettings);
		try {
			VaultState vaultState = determineVaultState(vaultSettings.path().get());
			compBuilder.initialVaultState(vaultState);
		} catch (IOException e) {
			LOG.warn("Failed to determine vault state for " + vaultSettings.path().get(), e);
			compBuilder.initialVaultState(VaultState.ERROR);
			compBuilder.initialErrorCause(e);
		}
		return compBuilder.build().vault();
	}

	public static VaultState determineVaultState(Path pathToVault) throws IOException {
		if (!CryptoFileSystemProvider.containsVault(pathToVault, MASTERKEY_FILENAME)) {
			return VaultState.MISSING;
		} else if (Migrators.get().needsMigration(pathToVault, MASTERKEY_FILENAME)) {
			return VaultState.NEEDS_MIGRATION;
		} else {
			return VaultState.LOCKED;
		}
	}

}
