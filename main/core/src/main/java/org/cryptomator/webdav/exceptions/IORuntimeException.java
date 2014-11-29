/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.exceptions;

import java.io.IOException;

public class IORuntimeException extends RuntimeException {

	private static final long serialVersionUID = -4713080133052143303L;

	public IORuntimeException(IOException ioException) {
		super(ioException);
	}

	@Override
	public String getMessage() {
		return getCause().getMessage();
	}

	@Override
	public String getLocalizedMessage() {
		return getCause().getLocalizedMessage();
	}

}
