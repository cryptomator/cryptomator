package org.cryptomator.common;

import java.io.IOException;

/**
 * Thrown by {@link CryptomatorMKFValidator} if the scrypt parameters of a masterkey file exceed the accepted limits.
 */
public class UnsupportedScryptParametersException extends IOException {

	UnsupportedScryptParametersException(String message) {
		super(message);
	}

	UnsupportedScryptParametersException(String message, Throwable cause) {
		super(message, cause);
	}

}
