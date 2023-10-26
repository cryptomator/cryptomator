package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class MountModule {

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		return MountService.get().toList();
	}

	@Provides
	@Singleton
	@Named("FUPFMS")
	static AtomicReference<MountService> provideFirstUsedProblematicFuseMountService() {
		return new AtomicReference<>(null);
	}
}
