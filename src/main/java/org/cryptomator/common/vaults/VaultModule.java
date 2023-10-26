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

	private static final AtomicReference<MountService> formerSelectedMountService = new AtomicReference<>(null);
	private static final List<String> problematicFuseMountServices = List.of("org.cryptomator.frontend.fuse.mount.MacFuseMountProvider", "org.cryptomator.frontend.fuse.mount.FuseTMountProvider");
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
		var observableMountService = ObservableUtil.mapWithDefault(vaultSettings.mountService, //
				desiredServiceImpl -> { //
					var serviceFromSettings = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny(); //
					var targetedService = serviceFromSettings.orElse(fallbackProvider);
					return applyWorkaroundForProblematicFuse(targetedService, serviceFromSettings.isPresent(), fupfms);
				}, //
				() -> { //
					return applyWorkaroundForProblematicFuse(fallbackProvider, true, fupfms);
				});
		return observableMountService;
	}

	//see https://github.com/cryptomator/cryptomator/issues/2786
	private synchronized static ActualMountService applyWorkaroundForProblematicFuse(MountService targetedService, boolean isDesired, AtomicReference<MountService> firstUsedProblematicFuseMountService) {
		//set the first used problematic fuse service if applicable
		var targetIsProblematicFuse = isProblematicFuseService(targetedService);
		if (targetIsProblematicFuse && firstUsedProblematicFuseMountService.get() == null) {
			firstUsedProblematicFuseMountService.set(targetedService);
		}

		//do not use the targeted mount service and fallback to former one, if the service is problematic _and_ not the first problematic one used.
		if (targetIsProblematicFuse && !firstUsedProblematicFuseMountService.get().equals(targetedService)) {
			return new ActualMountService(formerSelectedMountService.get(), false);
		} else {
			formerSelectedMountService.set(targetedService);
			return new ActualMountService(targetedService, isDesired);
		}
	}

	public static boolean isProblematicFuseService(MountService service) {
		return problematicFuseMountServices.contains(service.getClass().getName());
	}

	@Provides
	@Named("lastKnownException")
	@PerVault
	public ObjectProperty<Exception> provideLastKnownException(@Named("lastKnownException") @Nullable Exception initialErrorCause) {
		return new SimpleObjectProperty<>(initialErrorCause);
	}

}
