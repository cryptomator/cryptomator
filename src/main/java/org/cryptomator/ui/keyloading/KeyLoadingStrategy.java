package org.cryptomator.ui.keyloading;

import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;

import java.net.URI;

/**
 * A reusable, stateful {@link MasterkeyLoader}, that can deal with certain exceptions.
 */
@FunctionalInterface
public interface KeyLoadingStrategy extends MasterkeyLoader {

	/**
	 * Loads a master key. This might be a long-running operation, as it may require user input or expensive computations.
	 * <p>
	 * If loading fails exceptionally, this strategy might be able to {@link #recoverFromException(MasterkeyLoadingFailedException) recover from this exception}, so it can be used in a further attempt.
	 *
	 * @param keyId An URI uniquely identifying the source and identity of the key
	 * @return The raw key bytes. Must not be null
	 * @throws MasterkeyLoadingFailedException Thrown when it is impossible to fulfill the request
	 */
	@Override
	Masterkey loadKey(URI keyId) throws MasterkeyLoadingFailedException;

	/**
	 * Allows the loader to try and recover from an exception thrown during the last attempt.
	 *
	 * @param exception An exception thrown by {@link #loadKey(URI)}.
	 * @return <code>true</code> if this component was able to handle the exception and another attempt can be made to load a masterkey
	 */
	default boolean recoverFromException(MasterkeyLoadingFailedException exception) {
		return false;
	}

	/**
	 * Release any resources or do follow-up tasks after loading a key.
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
	 * @param exception The cause of the failure. If not already an {@link MasterkeyLoadingFailedException}, it will get wrapped.
	 * @return A new KeyLoadingStrategy that will always fail with an {@link MasterkeyLoadingFailedException}.
	 */
	static KeyLoadingStrategy failed(Exception exception) {
		return keyid -> {
			if (exception instanceof MasterkeyLoadingFailedException e) {
				throw e;
			} else {
				throw new MasterkeyLoadingFailedException("Can not load key", exception);
			}
		};
	}

}
