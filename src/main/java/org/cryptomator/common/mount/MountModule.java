package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class MountModule {

	private static final AtomicReference<MountService> formerSelectedMountService = new AtomicReference<>(null);
	private static final AtomicReference<MountService> firstUsedFuseMountService = new AtomicReference<>(null);

	@Provides
	@Singleton
	static List<MountService> provideSupportedMountServices() {
		return MountService.get().toList();
	}

	@Provides
	@Singleton
	static ObservableValue<ActualMountService> provideMountService(Settings settings, List<MountService> serviceImpls) {
		var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);

		var observableMountService = ObservableUtil.mapWithDefault(settings.mountService(), //
				desiredServiceImpl -> { //
					var serviceFromSettings = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny(); //
					var targetedService = serviceFromSettings.orElse(fallbackProvider);
					return applyWorkaroundForFuse(targetedService, serviceFromSettings.isPresent());
				}, //
				() -> { //
					return applyWorkaroundForFuse(fallbackProvider, true);
				});
		return observableMountService;
	}

	//see https://github.com/cryptomator/cryptomator/issues/2786
	private synchronized static ActualMountService applyWorkaroundForFuse(MountService targetedService, boolean isDesired) {
		//set the first used fuse service if applicable
		var targetIsFuse = isFuseService(targetedService);
		if (targetIsFuse && firstUsedFuseMountService.get() == null) {
			firstUsedFuseMountService.set(targetedService);
		}

		//make sure that the first used fuse service is always used
		if (targetIsFuse && !firstUsedFuseMountService.get().equals(targetedService)) {
			return new ActualMountService(formerSelectedMountService.get(), false);
		} else {
			formerSelectedMountService.set(targetedService);
			return new ActualMountService(targetedService, isDesired);
		}
	}

	private static boolean isFuseService(MountService service) {
		return service.getClass().getName().startsWith("org.cryptomator.frontend.fuse.mount.");
	}
}
