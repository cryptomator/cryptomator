/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.ui.CryptomatorComponent;
import org.cryptomator.ui.settings.VaultSettings;

@Singleton
public class VaultFactory {

	private final CryptomatorComponent cryptomatorComponent;
	private final ConcurrentMap<VaultSettings, Vault> vaults = new ConcurrentHashMap<>();

	@Inject
	public VaultFactory(CryptomatorComponent cryptomatorComponent) {
		this.cryptomatorComponent = cryptomatorComponent;
	}

	public Vault get(VaultSettings vaultSettings) {
		return vaults.computeIfAbsent(vaultSettings, this::create);
	}

	private Vault create(VaultSettings vaultSettings) {
		VaultModule module = new VaultModule(vaultSettings);
		VaultComponent comp = cryptomatorComponent.newVaultComponent(module);
		return comp.vault();
	}

}
