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
import java.util.concurrent.CountDownLatch;

@Module
class CryptomatorModule {

	@Provides
	@Singleton
	static Settings provideSettings(SettingsProvider settingsProvider) {
		return settingsProvider.get();
	}

	@Provides
	@Singleton
	@Named("launchEventQueue")
	static BlockingQueue<AppLaunchEvent> provideFileOpenRequests() {
		return new ArrayBlockingQueue<>(10);
	}

	@Provides
	@Singleton
	@Named("applicationVersion")
	static Optional<String> provideApplicationVersion() {
		return Optional.ofNullable(Cryptomator.class.getPackage().getImplementationVersion());
	}

}
