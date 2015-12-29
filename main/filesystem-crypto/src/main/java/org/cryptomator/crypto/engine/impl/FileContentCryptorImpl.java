/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;

class FileContentCryptorImpl implements FileContentCryptor {

	private final SecretKey encryptionKey;
	private final SecretKey macKey;
	private final SecureRandom randomSource;

	public FileContentCryptorImpl(SecretKey encryptionKey, SecretKey macKey, SecureRandom randomSource) {
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
		this.randomSource = randomSource;
	}

	@Override
	public int getHeaderSize() {
		return FileHeader.HEADER_SIZE;
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header) {
		if (header.remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header.");
		}
		return new FileContentDecryptorImpl(encryptionKey, macKey, header);
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header) {
		if (header.isPresent() && header.get().remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header.");
		}
		return new FileContentEncryptorImpl(encryptionKey, macKey, randomSource);
	}

}
