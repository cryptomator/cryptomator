package org.cryptomator.keychain;

interface KeychainAccessStrategy extends KeychainAccess {

	/**
	 * @return <code>true</code> if this KeychainAccessStrategy works on the current machine.
	 */
	boolean isSupported();

}
