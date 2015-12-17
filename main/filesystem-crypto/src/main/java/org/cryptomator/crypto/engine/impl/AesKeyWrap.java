/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

final class AesKeyWrap {

	private static final String RFC3394_CIPHER = "AESWrap";

	private AesKeyWrap() {
	}

	/**
	 * @param kek Key encrypting key
	 * @param key Key to be wrapped
	 * @return Wrapped key
	 */
	public static byte[] wrap(SecretKey kek, SecretKey key) {
		final Cipher cipher;
		try {
			cipher = Cipher.getInstance(RFC3394_CIPHER);
			cipher.init(Cipher.WRAP_MODE, kek);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException("Invalid key.", e);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new IllegalStateException("Algorithm/Padding should exist.", e);
		}

		try {
			return cipher.wrap(key);
		} catch (InvalidKeyException | IllegalBlockSizeException e) {
			throw new IllegalStateException("Unable to wrap key.", e);
		}
	}

	/**
	 * @param kek Key encrypting key
	 * @param wrappedKey Key to be unwrapped
	 * @param keyAlgorithm Key designation, i.e. algorithm name to be associated with the unwrapped key.
	 * @return Unwrapped key
	 * @throws NoSuchAlgorithmException If keyAlgorithm is unknown
	 * @throws InvalidKeyException If unwrapping failed (i.e. wrong kek)
	 */
	public static SecretKey unwrap(SecretKey kek, byte[] wrappedKey, String keyAlgorithm) throws InvalidKeyException, NoSuchAlgorithmException {
		final Cipher cipher;
		try {
			cipher = Cipher.getInstance(RFC3394_CIPHER);
			cipher.init(Cipher.UNWRAP_MODE, kek);
		} catch (InvalidKeyException ex) {
			throw new IllegalArgumentException("Invalid key.", ex);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
			throw new IllegalStateException("Algorithm/Padding should exist.", ex);
		}

		return (SecretKey) cipher.unwrap(wrappedKey, keyAlgorithm, Cipher.SECRET_KEY);
	}

}
