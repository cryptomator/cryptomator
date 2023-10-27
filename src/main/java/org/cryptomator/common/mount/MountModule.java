package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Singleton;
import java.util.List;

@Module
public class MountModule {

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		return MountService.get().toList();
	}

}
