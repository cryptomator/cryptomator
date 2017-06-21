/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.common.settings.VaultSettings;

@Singleton
public class VaultFactory {

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
		VaultModule module = new VaultModule(vaultSettings);
		VaultComponent comp = vaultComponentBuilder.vaultModule(module).build();
		return comp.vault();
	}

}
