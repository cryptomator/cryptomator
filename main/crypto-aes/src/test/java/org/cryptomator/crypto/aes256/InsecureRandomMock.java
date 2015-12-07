package org.cryptomator.crypto.aes256;

import java.security.SecureRandom;
import java.util.Random;

/**
 * <b>DO NOT USE</b>
 * 
 * This class is for testing only.
 */
@Deprecated // marked as deprecated and made package-private inside /src/test/java to avoid accidential use.
class InsecureRandomMock extends SecureRandom {

	private static final long serialVersionUID = 1505563778398085504L;
	private final Random random = new Random();

	@Override
	public void nextBytes(byte[] bytes) {
		// let the deterministic RNG do the work:
		this.random.nextBytes(bytes);
	}

}
