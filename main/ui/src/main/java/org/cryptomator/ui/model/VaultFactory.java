/*******************************************************************************
 * Copyright (c) 2016, 2017 Sebastian Stenzel and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the accompanying LICENSE file.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.ui.model;

import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.common.settings.VaultSettings;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@FxApplicationScoped
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
		VaultComponent comp = vaultComponentBuilder.vaultSettings(vaultSettings).build();
		return comp.vault();
	}

}
