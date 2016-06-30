package org.cryptomator.crypto.engine.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public final class Constants {

	private Constants() {
	}

	static final Collection<Integer> SUPPORTED_VAULT_VERSIONS = Collections.unmodifiableCollection(Arrays.asList(3, 4));
	static final Integer CURRENT_VAULT_VERSION = 4;

	public static final int PAYLOAD_SIZE = 32 * 1024;
	public static final int NONCE_SIZE = 16;
	public static final int MAC_SIZE = 32;
	public static final int CHUNK_SIZE = NONCE_SIZE + PAYLOAD_SIZE + MAC_SIZE;

}
