/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.security.SecureRandom;
import java.util.Arrays;

import org.cryptomator.crypto.engine.Cryptor;

public class TestCryptorImplFactory {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		private static final long serialVersionUID = 1505563778398085504L;

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	/**
	 * @return A CryptorImpl with a mocked PRNG, that can be used during tests without the need of "real" random numbers.
	 */
	public static Cryptor insecureCryptorImpl() {
		return new CryptorImpl(RANDOM_MOCK);
	}

}
