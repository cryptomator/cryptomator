package org.cryptomator.crypto.engine.impl;

import java.security.SecureRandom;
import java.util.Arrays;

import org.cryptomator.crypto.engine.Cryptor;

import dagger.Module;
import dagger.Provides;

@Module
public class CryptoTestModule {

	@Provides
	Cryptor provideCryptor(SecureRandom secureRandom) {
		return new CryptorImpl(secureRandom);
	}

	@Provides
	SecureRandom provideSecureRandom() {
		return new SecureRandom() {

			@Override
			public void nextBytes(byte[] bytes) {
				Arrays.fill(bytes, (byte) 0x00);
			}

		};
	}

}
