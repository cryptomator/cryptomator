/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto;

/**
 * Optional monitoring interface. If a cryptor implements this interface, it counts bytes de- and encrypted in a thread-safe manner.
 */
public interface CryptorIOSampling {

	/**
	 * @return Number of encrypted bytes since the last reset.
	 */
	long pollEncryptedBytes(boolean resetCounter);

	/**
	 * @return Number of decrypted bytes since the last reset.
	 */
	long pollDecryptedBytes(boolean resetCounter);

}
