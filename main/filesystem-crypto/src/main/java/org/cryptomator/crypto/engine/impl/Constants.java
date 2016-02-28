package org.cryptomator.crypto.engine.impl;

public final class Constants {

	private Constants() {
	}

	static final Integer CURRENT_VAULT_VERSION = 3;

	public static final int PAYLOAD_SIZE = 32 * 1024;
	public static final int NONCE_SIZE = 16;
	public static final int MAC_SIZE = 32;
	public static final int CHUNK_SIZE = NONCE_SIZE + PAYLOAD_SIZE + MAC_SIZE;

}
