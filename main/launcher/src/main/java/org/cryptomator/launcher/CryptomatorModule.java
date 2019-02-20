package org.cryptomator.launcher;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.ui.model.AppLaunchEvent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Module
class CryptomatorModule {

	@Provides
	@Singleton
	Settings provideSettings(SettingsProvider settingsProvider) {
		return settingsProvider.get();
	}

	@Provides
	@Singleton
	@Named("launchEventQueue")
	BlockingQueue<AppLaunchEvent> provideFileOpenRequests() {
		return new ArrayBlockingQueue<>(10);
	}

	@Provides
	@Singleton
	@Named("applicationVersion")
	Optional<String> provideApplicationVersion() {
		return Optional.ofNullable(Cryptomator.class.getPackage().getImplementationVersion());
	}

}
