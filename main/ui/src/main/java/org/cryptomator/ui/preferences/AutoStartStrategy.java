package org.cryptomator.ui.preferences;

public interface AutoStartStrategy {

	boolean isAutoStartEnabled();

	void enableAutoStart();

	void disableAutoStart();
}
