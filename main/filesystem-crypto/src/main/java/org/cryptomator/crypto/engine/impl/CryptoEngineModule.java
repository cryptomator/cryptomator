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
			// https://tersesystems.com/2015/12/17/the-right-way-to-use-securerandom/
			final SecureRandom nativeRandom = SecureRandom.getInstanceStrong();
			byte[] seed = nativeRandom.generateSeed(55); // NIST SP800-90A suggests 440 bits for SHA1 seed
			SecureRandom sha1Random = SecureRandom.getInstance("SHA1PRNG");
			sha1Random.setSeed(seed);
			return sha1Random;
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No strong PRNGs available.", e);
		}
	}

}
