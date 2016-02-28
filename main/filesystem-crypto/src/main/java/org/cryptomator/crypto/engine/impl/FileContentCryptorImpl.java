/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import static org.cryptomator.crypto.engine.impl.Constants.CHUNK_SIZE;
import static org.cryptomator.crypto.engine.impl.Constants.PAYLOAD_SIZE;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cryptomator.crypto.engine.AuthenticationFailedException;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;

class FileContentCryptorImpl implements FileContentCryptor {

	private final SecretKey encryptionKey;
	private final SecretKey macKey;
	private final SecureRandom randomSource;

	FileContentCryptorImpl(SecretKey encryptionKey, SecretKey macKey, SecureRandom randomSource) {
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
		this.randomSource = randomSource;
	}

	@Override
	public int getHeaderSize() {
		return FileHeader.HEADER_SIZE;
	}

	@Override
	public long toCiphertextPos(long cleartextPos) {
		long chunkNum = cleartextPos / PAYLOAD_SIZE;
		long cleartextChunkStart = chunkNum * PAYLOAD_SIZE;
		assert cleartextChunkStart <= cleartextPos;
		long chunkInternalDiff = cleartextPos - cleartextChunkStart;
		assert chunkInternalDiff >= 0 && chunkInternalDiff < PAYLOAD_SIZE;
		long ciphertextChunkStart = chunkNum * CHUNK_SIZE;
		return ciphertextChunkStart + chunkInternalDiff;
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header, long firstCiphertextByte, boolean authenticate) throws IllegalArgumentException, AuthenticationFailedException {
		if (header.remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header.");
		}
		if (firstCiphertextByte % CHUNK_SIZE != 0) {
			throw new IllegalArgumentException("Invalid starting point for decryption.");
		}
		return new FileContentDecryptorImpl(encryptionKey, macKey, header, firstCiphertextByte, authenticate);
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header, long firstCleartextByte) {
		if (header.isPresent() && header.get().remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header.");
		}
		if (firstCleartextByte % PAYLOAD_SIZE != 0) {
			throw new IllegalArgumentException("Invalid starting point for encryption.");
		}
		return new FileContentEncryptorImpl(encryptionKey, macKey, randomSource, firstCleartextByte);
	}

}
