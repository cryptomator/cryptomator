/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

interface KeychainAccessStrategy {

	/**
	 * Associates a passphrase with a given key.
	 *
	 * @param key Key used to retrieve the passphrase via {@link #loadPassphrase(String)}.
	 * @param passphrase The secret to store in this keychain.
	 */
	void storePassphrase(String key, CharSequence passphrase) throws KeychainAccessException;

	/**
	 * @param key Unique key previously used while {@link #storePassphrase(String, CharSequence) storing a passphrase}.
	 * @return The stored passphrase for the given key or <code>null</code> if no value for the given key could be found.
	 */
	char[] loadPassphrase(String key) throws KeychainAccessException;

	/**
	 * Deletes a passphrase with a given key.
	 *
	 * @param key Unique key previously used while {@link #storePassphrase(String, CharSequence) storing a passphrase}.
	 */
	void deletePassphrase(String key) throws KeychainAccessException;

	/**
	 * Updates a passphrase with a given key.
	 *
	 * @param key Unique key previously used while {@link #storePassphrase(String, CharSequence) storing a passphrase}.
	 * @param passphrase The secret to be updated in this keychain.
	 */
	void changePassphrase(String key, CharSequence passphrase) throws KeychainAccessException;

	/**
	 * @return <code>true</code> if this KeychainAccessStrategy works on the current machine.
	 * @implNote This method must not throw any exceptions and should fail fast
	 * returning <code>false</code> if it can't determine availability of the checked strategy
	 */
	boolean isSupported();

}
