/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

public interface KeychainAccess {

	/**
	 * Associates a passphrase with a given key.
	 * 
	 * @param key Key used to retrieve the passphrase via {@link #loadPassphrase(String)}.
	 * @param passphrase The secret to store in this keychain.
	 */
	void storePassphrase(String key, CharSequence passphrase);

	/**
	 * @param key Unique key previously used while {@link #storePassphrase(String, CharSequence) storing a passphrase}.
	 * @return The stored passphrase for the given key or <code>null</code> if no value for the given key could be found.
	 */
	char[] loadPassphrase(String key);

	/**
	 * Deletes a passphrase with a given key.
	 * 
	 * @param key Unique key previously used while {@link #storePassphrase(String, CharSequence) storing a passphrase}.
	 */
	void deletePassphrase(String key);

}
