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
	private static final AtomicReference<MountService> firstUsedProblematicFuseMountService = new AtomicReference<>(null);

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
					return applyWorkaroundForProblematicFuse(targetedService, serviceFromSettings.isPresent());
				}, //
				() -> { //
					return applyWorkaroundForProblematicFuse(fallbackProvider, true);
				});
		return observableMountService;
	}

	//see https://github.com/cryptomator/cryptomator/issues/2786
	private synchronized static ActualMountService applyWorkaroundForProblematicFuse(MountService targetedService, boolean isDesired) {
		//set the first used problematic fuse service if applicable
		var targetIsProblematicFuse = isProblematicFuseService(targetedService);
		if (targetIsProblematicFuse && firstUsedProblematicFuseMountService.get() == null) {
			firstUsedProblematicFuseMountService.set(targetedService);
		}

		//make sure that the first used problematic fuse service is always used
		if (targetIsProblematicFuse && !firstUsedProblematicFuseMountService.get().equals(targetedService)) {
			return new ActualMountService(formerSelectedMountService.get(), false);
		} else {
			formerSelectedMountService.set(targetedService);
			return new ActualMountService(targetedService, isDesired);
		}
	}

	private static boolean isProblematicFuseService(MountService service) {
		return List.of("org.cryptomator.frontend.fuse.mount.MacFuseMountProvider", "org.cryptomator.frontend.fuse.mount.FuseTMountProvider").contains(service.getClass().getName());
	}
}
