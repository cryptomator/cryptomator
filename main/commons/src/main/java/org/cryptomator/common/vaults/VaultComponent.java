/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import dagger.BindsInstance;
import org.cryptomator.common.settings.VaultSettings;

import dagger.Subcomponent;

import javax.annotation.Nullable;
import javax.inject.Named;

@PerVault
@Subcomponent(modules = {VaultModule.class})
public interface VaultComponent {

	Vault vault();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vaultSettings(VaultSettings vaultSettings);

		@BindsInstance
		Builder initialVaultState(VaultState vaultState);

		@BindsInstance
		Builder initialErrorCause(@Nullable @Named("lastKnownException") Exception initialErrorCause);

		VaultComponent build();
	}

}
