package org.cryptomator.ui.preferences;

import org.cryptomator.jni.MacFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class AutoStartMacStrategy implements AutoStartStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AutoStartMacStrategy.class);

	private final MacFunctions macFunctions;

	public AutoStartMacStrategy(MacFunctions macFunctions) {
		this.macFunctions = macFunctions;
	}

	@Override
	public CompletionStage<Boolean> isAutoStartEnabled() {
		boolean enabled = macFunctions.launchServices().isLoginItemEnabled();
		return CompletableFuture.completedFuture(enabled);
	}

	@Override
	public void enableAutoStart() throws TogglingAutoStartFailedException {
		if (macFunctions.launchServices().enableLoginItem()) {
			LOG.debug("Added login item.");
		} else {
			throw new TogglingAutoStartFailedException("Failed to add login item.");
		}
	}

	@Override
	public void disableAutoStart() throws TogglingAutoStartFailedException {
		if (macFunctions.launchServices().disableLoginItem()) {
			LOG.debug("Removed login item.");
		} else {
			throw new TogglingAutoStartFailedException("Failed to remove login item.");
		}
	}
}
