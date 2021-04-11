package org.cryptomator.ui.error;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;

@Subcomponent(modules = {ErrorModule.class, GenericErrorComponent.GenericErrorModule.class})
public interface GenericErrorComponent extends ErrorComponentBase {

	@Subcomponent.Builder
	interface Builder extends BuilderBase<Builder, GenericErrorComponent> { /* Handled by base interface */ }

	@Module
	abstract class GenericErrorModule {

		@Provides
		@ErrorReport
		static FxmlFile provideFxmlFile() {
			return FxmlFile.GENERIC_ERROR;
		}

		@Binds
		@IntoMap
		@FxControllerKey(GenericErrorController.class)
		abstract FxController bindController(GenericErrorController controller);
	}
}