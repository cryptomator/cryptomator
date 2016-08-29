package org.cryptomator.ui.jni;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MacFunctions {

	@Inject
	MacFunctions() {
	}

	/**
	 * Makes the current application a foreground application, which appears in the Dock and the Application Switcher.
	 */
	public void transformToForegroundApplication() {
		int errorCode = transformToForegroundApplication0();
		if (errorCode != 0) {
			throw new JniException("Failed to make app a foreground app. Error code " + errorCode);
		}
	}

	private native int transformToForegroundApplication0();

	/**
	 * Makes the current application an agent app. Agent apps do not appear in the Dock or in the Force Quit window.
	 */
	public void transformToAgentApplication() {
		int errorCode = transformToAgentApplication0();
		if (errorCode != 0) {
			throw new JniException("Failed to make app an agent app. Error code " + errorCode);
		}
	}

	private native int transformToAgentApplication0();

}
