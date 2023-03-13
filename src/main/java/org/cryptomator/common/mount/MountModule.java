package org.cryptomator.common.mount;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Module
public class MountModule {

	private static final AtomicReference<MountService> formerSelectedMountService = new AtomicReference<>(null);
	private static final AtomicBoolean MAC_FUSE_SELECTED_ONCE = new AtomicBoolean(false);
	private static final AtomicBoolean FUSET_SELECTED_ONCE = new AtomicBoolean(false);

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
					return applyWorkaroundForFuseTMacFuse(targetedService, serviceFromSettings.isPresent());
				}, //
				() -> { //
					return applyWorkaroundForFuseTMacFuse(fallbackProvider, true);
				});
		return observableMountService;
	}


	//see https://github.com/cryptomator/cryptomator/issues/2786
	private static ActualMountService applyWorkaroundForFuseTMacFuse(MountService targetedService, boolean isDesired) {
		//check if any of both were already used. If not, check if targetedService is macFuse or FUSE-T
		if (!MAC_FUSE_SELECTED_ONCE.get() && !FUSET_SELECTED_ONCE.get()) {
			if (isMacFuseService(targetedService)) {
				MAC_FUSE_SELECTED_ONCE.set(true);
			} else if (isFuseTService(targetedService)) {
				FUSET_SELECTED_ONCE.set(true);
			}
		}

		if ((MAC_FUSE_SELECTED_ONCE.get() && isFuseTService(targetedService)) //
				|| (FUSET_SELECTED_ONCE.get() && isMacFuseService(targetedService))) {
			return new ActualMountService(formerSelectedMountService.get(), false); //
		} else {
			formerSelectedMountService.set(targetedService);
			return new ActualMountService(targetedService, isDesired); //
		}
	}

	private static boolean isFuseTService(MountService service) {
		return "org.cryptomator.frontend.fuse.mount.FuseTMountProvider".equals(service.getClass().getName());
	}

	private static boolean isMacFuseService(MountService service) {
		return "org.cryptomator.frontend.fuse.mount.MacFuseMountProvider".equals(service.getClass().getName());
	}

}
