/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import java.io.IOException;

public class CryptoException extends IOException {

	private static final long serialVersionUID = -6536997506620449023L;

	public CryptoException(String message, Throwable cause) {
		super(message, cause);
	}

}
