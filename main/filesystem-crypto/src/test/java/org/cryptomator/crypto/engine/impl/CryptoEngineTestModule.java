package org.cryptomator.crypto.engine.impl;

import java.security.SecureRandom;
import java.util.Arrays;

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
