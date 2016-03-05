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
import java.nio.file.FileAlreadyExistsException;
import java.util.Optional;

import org.cryptomator.crypto.engine.Cryptor;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class CryptoFile extends CryptoNode implements File {

	public CryptoFile(CryptoFolder parent, String name, Cryptor cryptor) {
		super(parent, name, cryptor);
	}

	@Override
	protected Optional<String> encryptedName() {
		return parent().get().getDirectoryId().map(s -> s.getBytes(UTF_8)).map(parentDirId -> {
			return cryptor.getFilenameCryptor().encryptFilename(name(), parentDirId);
		});
	}

	@Override
	public ReadableFile openReadable() {
		boolean authenticate = !fileSystem().delegate().shouldSkipAuthentication(toString());
		ReadableFile physicalReadable = forceGetPhysicalFile().openReadable();
		boolean success = false;
		try {
			final ReadableFile result = new CryptoReadableFile(cryptor.getFileContentCryptor(), physicalReadable, authenticate, this::reportAuthError);
			success = true;
			return result;
		} finally {
			if (!success) {
				physicalReadable.close();
			}
		}
	}

	private void reportAuthError() {
		fileSystem().delegate().authenticationFailed(this.toString());
	}

	@Override
	public WritableFile openWritable() {
		if (parent.folder(name).exists()) {
			throw new UncheckedIOException(new FileAlreadyExistsException(toString()));
		}
		WritableFile physicalWrtiable = forceGetPhysicalFile().openWritable();
		boolean success = false;
		try {
			final WritableFile result = new CryptoWritableFile(cryptor.getFileContentCryptor(), physicalWrtiable);
			success = true;
			return result;
		} finally {
			if (!success) {
				physicalWrtiable.close();
			}
		}
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
	public void delete() throws UncheckedIOException {
		forceGetPhysicalFile().delete();
	}

	@Override
	public void moveTo(File destination) throws UncheckedIOException {
		if (destination instanceof CryptoFile) {
			CryptoFile dst = (CryptoFile) destination;
			forceGetPhysicalFile().moveTo(dst.forceGetPhysicalFile());
		} else {
			throw new IllegalArgumentException("Can not move CryptoFile to conventional File.");
		}
	}

}
