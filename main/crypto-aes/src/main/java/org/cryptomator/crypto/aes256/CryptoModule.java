package org.cryptomator.crypto.aes256;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.cryptomator.crypto.Cryptor;

import dagger.Module;
import dagger.Provides;

@Module
public class CryptoModule {

	@Provides
	SecureRandom provideRandomNumberGenerator() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException e) {
			// quote "Every implementation of the Java platform is required to support at least one strong SecureRandom implementation."
			throw new AssertionError("No SecureRandom implementation available.");
		}
	}

	@Provides
	public Cryptor provideCryptor(SecureRandom secureRandom) {
		return new Aes256Cryptor(secureRandom);
	}

}
