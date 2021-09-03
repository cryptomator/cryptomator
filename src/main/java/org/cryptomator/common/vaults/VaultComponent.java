/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import dagger.BindsInstance;
import dagger.Subcomponent;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.mountpoint.MountPointChooserModule;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.VaultConfig;

import javax.inject.Named;

@PerVault
@Subcomponent(modules = {VaultModule.class, MountPointChooserModule.class})
public interface VaultComponent {

	Vault vault();

	@Subcomponent.Builder
	interface Builder {

		@BindsInstance
		Builder vaultSettings(VaultSettings vaultSettings);

		@BindsInstance
		Builder vaultConfigWrapper(VaultConfigWrapper rapper);

		@BindsInstance
		Builder initialVaultState(VaultState.Value vaultState);

		@BindsInstance
		Builder initialErrorCause(@Nullable @Named("lastKnownException") Exception initialErrorCause);

		VaultComponent build();
	}

}
