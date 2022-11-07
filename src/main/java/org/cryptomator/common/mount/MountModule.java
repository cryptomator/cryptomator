package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.util.List;

@Module
public class MountModule {

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		return MountService.get().toList();
	}

	@Provides
	@Singleton
	static ObservableValue<MountService> provideMountService(Settings settings, List<MountService> serviceImpls) {
		return settings.mountService().map(desiredServiceImpl -> {
			var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);
			return serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny().orElse(fallbackProvider);
		});
	}

}
