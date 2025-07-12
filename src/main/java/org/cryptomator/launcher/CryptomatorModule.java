package org.cryptomator.launcher;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.cryptomator.ui.fxapp.FxApplicationComponent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Module(subcomponents = {FxApplicationComponent.class})
class CryptomatorModule {

	@Provides
	@Singleton
	static ResourceBundle provideLocalization() {
		return ResourceBundle.getBundle("i18n.strings");
	}

	@Provides
	@Singleton
	@Named("launchEventQueue")
	static BlockingQueue<AppLaunchEvent> provideFileOpenRequests() {
		return new ArrayBlockingQueue<>(10);
	}

	@Provides
	@Singleton
	static Optional<UiAppearanceProvider> provideAppearanceProvider() {
		return UiAppearanceProvider.get();
	}

	@Provides
	@Singleton
	static Optional<AutoStartProvider> provideAutostartProvider() {
		return AutoStartProvider.get();
	}

	@Provides
	@Singleton
	static Optional<TrayIntegrationProvider> provideTrayIntegrationProvider() {
		return TrayIntegrationProvider.get();
	}

}
