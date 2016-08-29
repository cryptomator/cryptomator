package org.cryptomator.ui.jni;

import java.util.Optional;

import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class JniModule {

	private static final Logger LOG = LoggerFactory.getLogger(JniModule.class);

	@Provides
	@Singleton
	Optional<MacFunctions> provideMacFunctions(MacFunctions macFunctions) {
		if (SystemUtils.IS_OS_MAC) {
			try {
				System.loadLibrary("MacFunctions");
				return Optional.of(macFunctions);
			} catch (UnsatisfiedLinkError e) {
				LOG.error("Could not load JNI lib from path {}", System.getProperty("java.library.path"));
			}
		}
		return Optional.empty();
	}

}
