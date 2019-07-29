package org.cryptomator.launcher;

import dagger.Module;
import dagger.Provides;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.SettingsProvider;
import org.cryptomator.ui.UiModule;
import org.cryptomator.ui.model.AppLaunchEvent;
import org.cryptomator.ui.model.VaultComponent;
import org.cryptomator.ui.traymenu.TrayMenuComponent;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

@Module(includes = {UiModule.class}, subcomponents = {VaultComponent.class, TrayMenuComponent.class})
class CryptomatorModule {

	@Provides
	@Singleton
	@Named("shutdownTaskScheduler")
	Consumer<Runnable> provideShutdownTaskScheduler(CleanShutdownPerformer shutdownPerformer) {
		return shutdownPerformer::scheduleShutdownTask;
	}

	@Provides
	@Singleton
	@Named("shutdownLatch")
	static CountDownLatch provideShutdownLatch() {
		return new CountDownLatch(1);
	}

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
