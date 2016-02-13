/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

import org.cryptomator.crypto.engine.impl.CryptoEngineModule;

/**
 * Used as drop-in-replacement for {@link CryptoEngineModule} during unit tests.
 */
public class CryptoEngineTestModule extends CryptoEngineModule {

	@Override
	public SecureRandom provideSecureRandom() {
		return new SecureRandom() {

			@Override
			public void nextBytes(byte[] bytes) {
				Arrays.fill(bytes, (byte) 0x00);
			}

		};
	}

}
