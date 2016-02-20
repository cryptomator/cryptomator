/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

abstract class CryptoException extends RuntimeException {

	public CryptoException() {
		super();
	}

	public CryptoException(String message) {
		super(message);
	}

	public CryptoException(Throwable cause) {
		super(cause);
	}

	public CryptoException(String message, Throwable cause) {
		super(message, cause);
	}

}
