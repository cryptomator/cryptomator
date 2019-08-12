package org.cryptomator.keychain;

/**
 * Indicates an error during communication with the operating system's keychain.
 */
public class KeychainAccessException extends Exception {
	
	KeychainAccessException(Throwable cause) {
		super(cause);
	}

}
