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
