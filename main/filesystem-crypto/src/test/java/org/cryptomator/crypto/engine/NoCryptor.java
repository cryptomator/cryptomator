/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

public class NoCryptor implements Cryptor {

	private final FilenameCryptor filenameCryptor = new NoFilenameCryptor();
	private final FileContentCryptor fileContentCryptor = new NoFileContentCryptor();

	@Override
	public FilenameCryptor getFilenameCryptor() {
		return filenameCryptor;
	}

	@Override
	public FileContentCryptor getFileContentCryptor() {
		return fileContentCryptor;
	}

	@Override
	public void randomizeMasterkey() {
		// like this? https://xkcd.com/221/
	}

	@Override
	public void readKeysFromMasterkeyFile(byte[] masterkeyFileContents, CharSequence passphrase) {
		// thanks, but I don't need a key, if I'm not encryption anything...
	}

	@Override
	public byte[] writeKeysToMasterkeyFile(CharSequence passphrase) {
		// ok, if you insist to get my non-existing key data... here you go:
		return new byte[0];
	}

	@Override
	public void destroy() {
		// no-op
	}

}
