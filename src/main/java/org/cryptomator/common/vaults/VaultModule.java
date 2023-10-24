/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.vaults;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.Nullable;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.mount.ActualMountService;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.integrations.mount.MountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class VaultModule {

	private static final Logger LOG = LoggerFactory.getLogger(VaultModule.class);

	@Provides
	@PerVault
	public AtomicReference<CryptoFileSystem> provideCryptoFileSystemReference() {
		return new AtomicReference<>();
	}

	@Provides
	@Named("vaultMountService")
	@PerVault
	static ObservableValue<ActualMountService> provideMountService(VaultSettings vaultSettings, List<MountService> serviceImpls, @Named("FUPFMS") AtomicReference<MountService> fupfms) {
		var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);

		LOG.debug("fallbackProvider.displayName:" + fallbackProvider.displayName());

		var observableMountService = ObservableUtil.mapWithDefault(vaultSettings.mountService, //
				desiredServiceImpl -> { //
					var serviceFromSettings = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny(); //
					var targetedService = serviceFromSettings.orElse(fallbackProvider);
					return new ActualMountService(targetedService,false);//return applyWorkaroundForProblematicFuse(targetedService, serviceFromSettings.isPresent(), fupfms);
				}, //
				() -> { //
					return new ActualMountService(fallbackProvider,false);//return applyWorkaroundForProblematicFuse(fallbackProvider, true, fupfms);
				});
		return observableMountService;
	}

	@Provides
	@Named("lastKnownException")
	@PerVault
	public ObjectProperty<Exception> provideLastKnownException(@Named("lastKnownException") @Nullable Exception initialErrorCause) {
		return new SimpleObjectProperty<>(initialErrorCause);
	}

}
