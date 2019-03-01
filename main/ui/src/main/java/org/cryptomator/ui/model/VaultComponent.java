/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.ui.model;

import dagger.BindsInstance;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.model.VaultModule.PerVault;

import dagger.Subcomponent;

@PerVault
@Subcomponent(modules = {VaultModule.class})
public interface VaultComponent {

	Vault vault();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vaultSettings(VaultSettings vaultSettings);

		VaultComponent build();
	}

}
