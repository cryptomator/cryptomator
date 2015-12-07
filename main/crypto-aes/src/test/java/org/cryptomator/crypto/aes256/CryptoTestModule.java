package org.cryptomator.crypto.aes256;

import java.security.SecureRandom;

import org.cryptomator.crypto.Cryptor;

import dagger.Module;
import dagger.Provides;

@Module
public class CryptoTestModule {

	@Provides
	@SuppressWarnings("deprecation")
	SecureRandom provideRandomNumberGenerator() {
		// we use this class for testing only, as unit tests on CI servers tend to stall, if they rely on true randomness.
		return new InsecureRandomMock();
	}

	@Provides
	Cryptor provideCryptor(SecureRandom secureRandom) {
		return new Aes256Cryptor(secureRandom);
	}

}
