package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Singleton;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import java.util.List;

@Module
public class MountModule {

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		return MountService.get().toList();
	}

	//currently not used, because macFUSE and FUSE-T cannot be used in the same JVM
	/*
	@Provides
	@Singleton
	static ObservableValue<ActualMountService> provideMountService(Settings settings, List<MountService> serviceImpls) {
		var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);
		return ObservableUtil.mapWithDefault(settings.mountService(), //
				desiredServiceImpl -> { //
					var desiredService = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny(); //
					return new ActualMountService(desiredService.orElse(fallbackProvider), desiredService.isPresent()); //
				}, //
				new ActualMountService(fallbackProvider, true));
	}
	 */

	@Provides
	@Singleton
	static ActualMountService provideActualMountService(Settings settings, List<MountService> serviceImpls) {
		var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);
		var desiredService = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(settings.mountService().getValue())).findFirst(); //
		return new ActualMountService(desiredService.orElse(fallbackProvider), desiredService.isPresent()); //
	}

	@Provides
	@Singleton
	static ObservableValue<ActualMountService> provideMountService(ActualMountService service) {
		return new SimpleObjectProperty<>(service);
	}

}
