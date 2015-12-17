/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.fs;

import java.io.UncheckedIOException;
import java.time.Instant;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

public class CryptoFile extends CryptoNode implements File {

	static final String FILE_EXT = ".file";

	public CryptoFile(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	String encryptedName() {
		return cryptor.getFilenameCryptor().encryptFilename(name()) + FILE_EXT;
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReadableFile openReadable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WritableFile openWritable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return parent.toString() + name;
	}

	@Override
	public int compareTo(File o) {
		return toString().compareTo(o.toString());
	}

}
