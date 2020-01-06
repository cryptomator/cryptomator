package org.cryptomator.ui.preferences;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacFunctions;

import java.util.Optional;

@Module
abstract class AutoStartModule {

	@Provides
	@PreferencesScoped
	public static Optional<AutoStartStrategy> provideAutoStartStrategy(Optional<MacFunctions> macFunctions) {
		if (SystemUtils.IS_OS_MAC_OSX && macFunctions.isPresent()) {
			return Optional.of(new AutoStartMacStrategy(macFunctions.get()));
		} else if (SystemUtils.IS_OS_WINDOWS) {
			// TODO: add windows support
			return Optional.empty();
		} else {
			return Optional.empty();
		}
	}

}
