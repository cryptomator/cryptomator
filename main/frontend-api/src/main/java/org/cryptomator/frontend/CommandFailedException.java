/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *     Markus Kreusch - Refactored WebDavMounter to use strategy pattern
 ******************************************************************************/
package org.cryptomator.frontend;

public class CommandFailedException extends Exception {

	private static final long serialVersionUID = 5784853630182321479L;

	public CommandFailedException(String message) {
		super(message);
	}

	public CommandFailedException(Throwable cause) {
		super(cause);
	}

}