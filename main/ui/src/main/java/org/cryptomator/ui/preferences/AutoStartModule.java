package org.cryptomator.ui.preferences;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.jni.MacFunctions;

import java.util.Optional;

@Module
abstract class AutoStartModule {

	@Provides
	@PreferencesScoped
	public static Optional<AutoStartStrategy> provideAutoStartStrategy(Optional<MacFunctions> macFunctions, Environment env) {
		if (SystemUtils.IS_OS_MAC_OSX && macFunctions.isPresent()) {
			return Optional.of(new AutoStartMacStrategy(macFunctions.get()));
		} else if (SystemUtils.IS_OS_WINDOWS) {
			Optional<String> exeName = ProcessHandle.current().info().command();
			return exeName.map(AutoStartWinStrategy::new);
		} else {
			return Optional.empty();
		}
	}

}
