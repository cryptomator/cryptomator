package org.cryptomator.ui.preferences;

import org.cryptomator.integrations.autostart.AutoStartProvider;
import org.cryptomator.integrations.autostart.ToggleAutoStartFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Deprecated
class AutoStartMacStrategy implements AutoStartStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AutoStartMacStrategy.class);

	private final AutoStartProvider autoStartProvider;

	public AutoStartMacStrategy(AutoStartProvider autoStartProvider) {
		this.autoStartProvider = autoStartProvider;
	}

	@Override
	public CompletionStage<Boolean> isAutoStartEnabled() {
		return CompletableFuture.completedFuture(autoStartProvider.isEnabled());
	}

	@Override
	public void enableAutoStart() throws TogglingAutoStartFailedException {
		try {
			autoStartProvider.enable();
			LOG.debug("Added login item.");
		} catch (ToggleAutoStartFailedException e) {
			throw new TogglingAutoStartFailedException("Failed to add login item.");
		}
	}

	@Override
	public void disableAutoStart() throws TogglingAutoStartFailedException {
		try {
			autoStartProvider.disable();
			LOG.debug("Removed login item.");
		} catch (ToggleAutoStartFailedException e) {
			throw new TogglingAutoStartFailedException("Failed to remove login item.");
		}
	}
}
