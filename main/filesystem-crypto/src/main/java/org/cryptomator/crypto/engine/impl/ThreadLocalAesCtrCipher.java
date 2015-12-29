/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

final class ThreadLocalAesCtrCipher {

	private ThreadLocalAesCtrCipher() {
	}

	private static final String AES_CTR = "AES/CTR/NoPadding";
	private static final ThreadLocal<Cipher> THREAD_LOCAL_CIPHER = ThreadLocal.withInitial(ThreadLocalAesCtrCipher::newCipherInstance);

	private static Cipher newCipherInstance() {
		try {
			return Cipher.getInstance(AES_CTR);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new IllegalStateException("Could not create MAC.", e);
		}
	}

	public static Cipher get() {
		return THREAD_LOCAL_CIPHER.get();
	}

}