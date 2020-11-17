package org.cryptomator.common.keychain;

import org.cryptomator.integrations.keychain.KeychainAccessException;

/**
 * Thrown by {@link KeychainManager} if attempted to access a keychain despite no supported keychain access provider being available.
 */
public class NoKeychainAccessProviderException extends KeychainAccessException {

	public NoKeychainAccessProviderException() {
		super("Did not find any supported keychain access provider.");
	}
}
