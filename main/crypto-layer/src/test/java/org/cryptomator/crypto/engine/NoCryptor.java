/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

public class NoCryptor implements Cryptor {

	private final FilenameCryptor filenameCryptor = new NoFilenameCryptor();

	@Override
	public FilenameCryptor getFilenameCryptor() {
		return filenameCryptor;
	}

}
