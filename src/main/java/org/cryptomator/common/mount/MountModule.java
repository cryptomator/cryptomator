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

import static org.cryptomator.common.mount.MountModule.FirstUsedFuseOnMacOS.FUSET;
import static org.cryptomator.common.mount.MountModule.FirstUsedFuseOnMacOS.MACFUSE;
import static org.cryptomator.common.mount.MountModule.FirstUsedFuseOnMacOS.UNDEFINED;

@Module
public class MountModule {

	private static final AtomicReference<MountService> formerSelectedMountService = new AtomicReference<>(null);
	private static final AtomicReference<FirstUsedFuseOnMacOS> FIRST_USED = new AtomicReference<>(UNDEFINED);

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
	private synchronized static ActualMountService applyWorkaroundForFuseTMacFuse(MountService targetedService, boolean isDesired) {
		var targetIsFuseT= isFuseTService(targetedService);
		var targetIsMacFuse= isMacFuseService(targetedService);

		//if none of macFUSE and FUSE-T were selected before, check if targetedService is macFUSE or FUSE-T
		if (FIRST_USED.get() == UNDEFINED) {
			if (targetIsMacFuse) {
				FIRST_USED.set(MAC_FUSE);
			} else if (targetIsFuseT) {
				FIRST_USED.set(FUSE_T);
			}
		}

		//if one of both were selected before and now the other should be used
		if ((FIRST_USED.get() == MAC_FUSE && targetIsFuseT) || (FIRST_USED.get() == FUSE_T && targetIsMacFuse )) {
			//return the former mount service
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

	private enum FirstUsedFuseOnMacOS {
		UNDEFINED,
		MAC_FUSE,
		FUSE_T;
	}
}
