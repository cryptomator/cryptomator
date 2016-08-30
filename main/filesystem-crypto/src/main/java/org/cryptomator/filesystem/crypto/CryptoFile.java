/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import static org.cryptomator.crypto.engine.impl.Constants.CHUNK_SIZE;
import static org.cryptomator.crypto.engine.impl.Constants.PAYLOAD_SIZE;

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
		return parent().get().encryptChildName(name());
	}

	@Override
	public long size() throws UncheckedIOException {
		if (!physicalFile().isPresent()) {
			return -1l;
		} else {
			File file = physicalFile().get();
			long ciphertextSize = file.size() - cryptor.getFileContentCryptor().getHeaderSize();
			long overheadPerChunk = CHUNK_SIZE - PAYLOAD_SIZE;
			long numFullChunks = ciphertextSize / CHUNK_SIZE; // floor by int-truncation
			long additionalCiphertextBytes = ciphertextSize % CHUNK_SIZE;
			if (additionalCiphertextBytes > 0 && additionalCiphertextBytes <= overheadPerChunk) {
				throw new IllegalArgumentException("Method not defined for input value " + ciphertextSize);
			}
			long additionalCleartextBytes = (additionalCiphertextBytes == 0) ? 0 : additionalCiphertextBytes - overheadPerChunk;
			assert additionalCleartextBytes >= 0;
			return PAYLOAD_SIZE * numFullChunks + additionalCleartextBytes;
		}
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
