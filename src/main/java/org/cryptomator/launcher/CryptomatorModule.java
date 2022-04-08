package org.cryptomator.launcher;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.PluginClassLoader;
import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.tray.TrayIntegrationProvider;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.cryptomator.ui.fxapp.FxApplicationComponent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
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

	// TODO: still needed after integrations-api 1.1.0?

	@Provides
	@Singleton
	static Optional<UiAppearanceProvider> provideAppearanceProvider(PluginClassLoader classLoader) {
		return ServiceLoader.load(UiAppearanceProvider.class, classLoader).findFirst();
	}

	@Provides
	@Singleton
	static Optional<AutoStartProvider> provideAutostartProvider(PluginClassLoader classLoader) {
		return ServiceLoader.load(AutoStartProvider.class, classLoader).findFirst();
	}

	@Provides
	@Singleton
	static Optional<TrayIntegrationProvider> provideTrayIntegrationProvider(PluginClassLoader classLoader) {
		return ServiceLoader.load(TrayIntegrationProvider.class, classLoader).findFirst();
	}


}
