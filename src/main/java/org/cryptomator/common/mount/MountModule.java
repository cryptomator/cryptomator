package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Named;
import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class MountModule {

	private static final AtomicReference<MountService> formerSelectedMountService = new AtomicReference<>(null);
	private static final List<String> problematicFuseMountServices = List.of("org.cryptomator.frontend.fuse.mount.MacFuseMountProvider", "org.cryptomator.frontend.fuse.mount.FuseTMountProvider");

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

	@Provides
	@Singleton
	static ObservableValue<ActualMountService> provideMountService(Settings settings, List<MountService> serviceImpls, @Named("FUPFMS") AtomicReference<MountService> fupfms) {
		var fallbackProvider = serviceImpls.stream().findFirst().orElse(null);

		var observableMountService = ObservableUtil.mapWithDefault(settings.mountService, //
				desiredServiceImpl -> { //
					var serviceFromSettings = serviceImpls.stream().filter(serviceImpl -> serviceImpl.getClass().getName().equals(desiredServiceImpl)).findAny(); //
					var targetedService = serviceFromSettings.orElse(fallbackProvider);
					return applyProblematicFuseWorkaround(targetedService, serviceFromSettings.isPresent(), fupfms);
				}, //
				() -> { //
					return applyProblematicFuseWorkaround(fallbackProvider, true, fupfms);
				});
		return observableMountService;
	}

	//see https://github.com/cryptomator/cryptomator/issues/2786
	// changed method name and variable name to remove the long identifier smell using Rename method / variable technique
	private synchronized static ActualMountService applyProblematicFuseWorkaround(MountService targetedService, boolean isDesired, AtomicReference<MountService> firstProblematicFuseService) {
		//set the first used problematic fuse service if applicable
		var targetIsProblematicFuse = isProblematicFuseService(targetedService);
		if (targetIsProblematicFuse && firstProblematicFuseService.get() == null) {
			firstProblematicFuseService.set(targetedService);
		}

		//do not use the targeted mount service and fallback to former one, if the service is problematic _and_ not the first problematic one used.
		if (targetIsProblematicFuse && !firstProblematicFuseService.get().equals(targetedService)) {
			return new ActualMountService(formerSelectedMountService.get(), false);
		} else {
			formerSelectedMountService.set(targetedService);
			return new ActualMountService(targetedService, isDesired);
		}
	}

	public static boolean isProblematicFuseService(MountService service) {
		return problematicFuseMountServices.contains(service.getClass().getName());
	}
}
