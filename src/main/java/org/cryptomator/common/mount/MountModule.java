package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Named;
import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Module
public class MountModule {

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		List<MountService> services = new ArrayList<>();
		ServiceLoader<MountService> loader = ServiceLoader.load(MountService.class);
		
		for (var iterator = loader.iterator(); iterator.hasNext();) {
			try {
				services.add(iterator.next());
			} catch (ServiceConfigurationError e) {
				// Skip services that can't be loaded (e.g., platform-specific services on wrong platform)
				System.err.println("Skipping mount service due to error: " + e.getMessage());
			}
		}
		
		return services;
	}

	@Provides
	@Singleton
	static ObservableValue<MountService> provideDefaultMountService(List<MountService> mountProviders, Settings settings) {
		var fallbackProvider = mountProviders.stream().findFirst().get(); //there should always be a mount provider, at least webDAV
		return ObservableUtil.mapWithDefault(settings.mountService, //
				serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), //
				fallbackProvider);
	}

	@Provides
	@Singleton
	@Named("usedMountServices")
	static Set<MountService> provideSetOfUsedMountServices() {
		return ConcurrentHashMap.newKeySet();
	}

}