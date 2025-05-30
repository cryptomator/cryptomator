package org.cryptomator.ui.traymenu;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.integrations.tray.TrayMenuController;

import java.util.Optional;

@Module
public class TrayMenuModule {

	@Provides
	@TrayMenuScoped
	static Optional<TrayMenuController> provideFirstSupportedTrayMenuController() {
		return TrayMenuController.get();
	}

}
