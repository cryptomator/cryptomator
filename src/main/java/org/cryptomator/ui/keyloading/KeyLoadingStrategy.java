package org.cryptomator.ui.keyloading;

import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;
import org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * A reusable, stateful {@link MasterkeyLoader}, that can deal with certain exceptions.
 */
@FunctionalInterface
public interface KeyLoadingStrategy extends MasterkeyLoader {

	Logger LOG = LoggerFactory.getLogger(KeyLoadingStrategy.class);

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
	 * Determines whether the provided key loader scheme corresponds to a Hub Vault.
	 * <p>
	 * This method compares the {@code keyLoader} parameter with the known Hub Vault schemes
	 * {@link HubKeyLoadingStrategy#SCHEME_HUB_HTTP} and {@link HubKeyLoadingStrategy#SCHEME_HUB_HTTPS}.
	 *
	 * @param keyLoader A string representing the key loader scheme to be checked.
	 * @return {@code true} if the given key loader scheme represents a Hub Vault; {@code false} otherwise.
	 */
	static boolean isHubVault(String keyLoader) {
		return HubKeyLoadingStrategy.SCHEME_HUB_HTTP.equals(keyLoader) || HubKeyLoadingStrategy.SCHEME_HUB_HTTPS.equals(keyLoader);
	}

	/**
	 * Determines whether the provided key loader scheme corresponds to a Masterkey File Vault.
	 * <p>
	 * This method checks if the {@code keyLoader} parameter matches the known Masterkey File Vault scheme
	 * {@link MasterkeyFileLoadingStrategy#SCHEME}.
	 * </p>
	 *
	 * @param keyLoader A string representing the key loader scheme to be checked.
	 * @return {@code true} if the given key loader scheme represents a Masterkey File Vault; {@code false} otherwise.
	 */
	static boolean isMasterkeyFileVault(String keyLoader) {
		return MasterkeyFileLoadingStrategy.SCHEME.equals(keyLoader);
	}

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

	/**
	 * Makes the given <code>user</code> apply this key loading strategy. If the user fails with a {@link MasterkeyLoadingFailedException},
	 * an attempt is made to {@link #recoverFromException(MasterkeyLoadingFailedException) recover} from it. Any other exception will be rethrown.
	 *
	 * @param user Some method using this strategy. May be invoked multiple times in case of recoverable {@link MasterkeyLoadingFailedException}s
	 * @param <E> Optional exception type thrown by <code>user</code>
	 * @throws MasterkeyLoadingFailedException If a non-recoverable exception is thrown by <code>user</code>
	 * @throws E Exception thrown by <code>user</code> and rethrown by this method
	 */
	default <E extends Exception> void use(KeyLoadingStrategyUser<E> user) throws MasterkeyLoadingFailedException, E {
		boolean success = false;
		try {
			user.use(this);
			success = true;
		} catch (MasterkeyLoadingFailedException e) {
			if (recoverFromException(e)) {
				LOG.info("Unlock attempt threw {}. Reattempting...", e.getClass().getSimpleName());
				use(user);
			} else {
				throw e;
			}
		} finally {
			cleanup(success);
		}
	}

	@FunctionalInterface
	interface KeyLoadingStrategyUser<E extends Exception> {

		void use(KeyLoadingStrategy strategy) throws MasterkeyLoadingFailedException, E;

	}

}
