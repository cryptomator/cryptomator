/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.common.vaults;

import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.migration.Migrators;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class VaultFactory {
	
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes

	private final VaultComponent.Builder vaultComponentBuilder;
	private final ConcurrentMap<VaultSettings, Vault> vaults = new ConcurrentHashMap<>();

	@Inject
	public VaultFactory(VaultComponent.Builder vaultComponentBuilder) {
		this.vaultComponentBuilder = vaultComponentBuilder;
	}

	public Vault get(VaultSettings vaultSettings) {
		return vaults.computeIfAbsent(vaultSettings, this::create);
	}

	private Vault create(VaultSettings vaultSettings) {
		VaultState vaultState = determineVaultState(vaultSettings.path().get());
		VaultComponent comp = vaultComponentBuilder.vaultSettings(vaultSettings).initialVaultState(vaultState).build();
		return comp.vault();
	}

	private VaultState determineVaultState(Path pathToVault) {
		try {
			if (!CryptoFileSystemProvider.containsVault(pathToVault, MASTERKEY_FILENAME)) {
				return VaultState.MISSING;
			} else if (Migrators.get().needsMigration(pathToVault, MASTERKEY_FILENAME)) {
				return VaultState.NEEDS_MIGRATION;
			} else {
				return VaultState.LOCKED;
			}
		} catch (IOException e) {
			return VaultState.ERROR;
		}
	}

}
