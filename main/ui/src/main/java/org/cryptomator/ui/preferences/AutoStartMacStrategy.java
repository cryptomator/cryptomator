package org.cryptomator.ui.preferences;

import org.cryptomator.jni.MacFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AutoStartMacStrategy implements AutoStartStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(AutoStartMacStrategy.class);
	
	private final MacFunctions macFunctions;

	public AutoStartMacStrategy(MacFunctions macFunctions) {
		this.macFunctions = macFunctions;
	}

	@Override
	public boolean isAutoStartEnabled() {
		return macFunctions.launchServices().isLoginItemEnabled();
	}

	@Override
	public void enableAutoStart() {
		if (macFunctions.launchServices().enableLoginItem()) {
			LOG.debug("Added login item.");
		} else {
			LOG.warn("Adding login item failed.");
		}
	}

	@Override
	public void disableAutoStart() {
		if (macFunctions.launchServices().disableLoginItem()) {
			LOG.debug("Removed login item.");
		} else {
			LOG.warn("Removing login item failed.");
		}
	}
}
