package org.cryptomator.ui.keyloading;

import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;

public interface KeyLoadingStrategy {

	/**
	 * @return A reusable masterkey loader, preconfigured with the vault of the current unlock process
	 * @throws MasterkeyLoadingFailedException If unable to provide the masterkey loader
	 */
	MasterkeyLoader masterkeyLoader() throws MasterkeyLoadingFailedException;

	/**
	 * Allows the component to try and recover from an exception thrown while loading a masterkey.
	 *
	 * @param exception An exception thrown either by {@link #masterkeyLoader()} or by the returned {@link MasterkeyLoader}.
	 * @return <code>true</code> if this component was able to handle the exception and another attempt should be made to load a masterkey
	 */
	default boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		return false;
	}

	/**
	 * Release any ressources or do follow-up tasks after loading a key.
	 *
	 * @param unlockedSuccessfully <code>true</code> if successfully unlocked a vault with the loaded key
	 * @implNote This method might be invoked multiple times, depending on whether multiple attempts to load a key are started.
	 */
	default void cleanup(boolean unlockedSuccessfully) {
		// no-op
	}

	/**
	 * A key loading strategy that will always fail by throwing a {@link MasterkeyLoadingFailedException}.
	 *
	 * @param exception The cause of the failure. If not alreay an {@link MasterkeyLoadingFailedException}, it will get wrapped.
	 * @return A new KeyLoadingStrategy that will always fail with an {@link MasterkeyLoadingFailedException}.
	 */
	static KeyLoadingStrategy failed(Exception exception) {
		return () -> {
			if (exception instanceof MasterkeyLoadingFailedException e) {
				throw e;
			} else {
				throw new MasterkeyLoadingFailedException("Can not load key", exception);
			}
		};
	}

}
