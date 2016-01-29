/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

public class CryptoFile extends CryptoNode implements File {

	static final String FILE_EXT = ".file";

	public CryptoFile(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected String encryptedName() {
		final byte[] parentDirId = parent.getDirectoryId().getBytes(UTF_8);
		return cryptor.getFilenameCryptor().encryptFilename(name(), parentDirId) + FILE_EXT;
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		return physicalFile().lastModified();
	}

	@Override
	public ReadableFile openReadable() {
		boolean authenticate = !fileSystem().delegate().shouldSkipAuthentication(toString());
		return new CryptoReadableFile(cryptor.getFileContentCryptor(), physicalFile().openReadable(), authenticate);
	}

	@Override
	public WritableFile openWritable() {
		return new CryptoWritableFile(cryptor.getFileContentCryptor(), physicalFile().openWritable());
	}

	@Override
	public String toString() {
		return parent.toString() + name;
	}

	@Override
	public int compareTo(File o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		return physicalFile().creationTime();
	}

}
