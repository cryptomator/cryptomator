package org.cryptomator.ui.preferences;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.integrations.autostart.AutoStartProvider;

import java.util.Optional;

@Deprecated
@Module
abstract class AutoStartModule {

	@Provides
	@PreferencesScoped
	public static Optional<AutoStartStrategy> provideAutoStartStrategy(Optional<AutoStartProvider> autoStartProvider) {
		if (SystemUtils.IS_OS_MAC_OSX && autoStartProvider.isPresent()) {
			return Optional.of(new AutoStartMacStrategy(autoStartProvider.get()));
		} else if (SystemUtils.IS_OS_WINDOWS) {
			Optional<String> exeName = ProcessHandle.current().info().command();
			return exeName.map(AutoStartWinStrategy::new);
		} else {
			return Optional.empty();
		}
	}

}
