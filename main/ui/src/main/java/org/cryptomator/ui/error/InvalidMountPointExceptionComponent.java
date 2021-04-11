package org.cryptomator.ui.error;

import dagger.Binds;
import dagger.BindsInstance;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.unlock.UnlockInvalidMountPointController;
import org.cryptomator.ui.unlock.UnlockScoped;

@Subcomponent(modules = {ErrorModule.class, InvalidMountPointExceptionComponent.InvalidMountPointExceptionModule.class})
@UnlockScoped
public interface InvalidMountPointExceptionComponent extends ErrorComponentBase {

	@Subcomponent.Builder
	interface Builder extends BuilderBase<Builder, InvalidMountPointExceptionComponent> {

		@BindsInstance
		Builder vault(@ErrorReport Vault vault);

	}

	@Module
	abstract class InvalidMountPointExceptionModule {

		@Provides
		@ErrorReport
		static FxmlFile provideFxmlFile() {
			return FxmlFile.UNLOCK_INVALID_MOUNT_POINT;
		}

		@Binds
		@IntoMap
		@FxControllerKey(UnlockInvalidMountPointController.class)
		abstract FxController bindController(UnlockInvalidMountPointController controller);
	}
}