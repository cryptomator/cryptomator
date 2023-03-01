/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.settings.VaultSettings;

import javax.inject.Named;

@PerVault
@Subcomponent(modules = {VaultModule.class})
public interface VaultComponent {

	Vault vault();

	@Subcomponent.Factory
	interface Factory {

		VaultComponent create(@BindsInstance VaultSettings vaultSettings, //
							  @BindsInstance VaultConfigCache configCache, //
							  @BindsInstance VaultState.Value vaultState, //
							  @BindsInstance @Nullable @Named("lastKnownException") Exception initialErrorCause);

	}
}