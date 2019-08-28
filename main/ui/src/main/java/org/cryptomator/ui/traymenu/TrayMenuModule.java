package org.cryptomator.ui.traymenu;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.JniModule;
import org.cryptomator.ui.fxapp.FxApplicationComponent;

import java.util.ResourceBundle;

@Module(includes = {JniModule.class}, subcomponents = {FxApplicationComponent.class})
abstract class TrayMenuModule {

	@Provides
	@TrayMenuScoped
	static ResourceBundle provideLocalization() {
		return ResourceBundle.getBundle("i18n.strings");
	}

}
