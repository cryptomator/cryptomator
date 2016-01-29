/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.cryptomator.crypto.engine.Cryptor;

import dagger.Module;
import dagger.Provides;

@Module
public class CryptoEngineModule {

	@Provides
	public Cryptor provideCryptor(SecureRandom secureRandom) {
		return new CryptorImpl(secureRandom);
	}

	@Provides
	public SecureRandom provideSecureRandom() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No strong PRNGs available.", e);
		}
	}

}
