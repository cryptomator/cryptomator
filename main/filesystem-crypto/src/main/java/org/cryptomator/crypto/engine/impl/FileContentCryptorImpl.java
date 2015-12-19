package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;

class FileContentCryptorImpl implements FileContentCryptor {

	private final SecretKey encryptionKey;
	private final SecretKey macKey;

	public FileContentCryptorImpl(SecretKey encryptionKey, SecretKey macKey) {
		if (encryptionKey == null || macKey == null) {
			throw new IllegalArgumentException("Key must not be null");
		}
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
	}

	@Override
	public int getHeaderSize() {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header) {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header) {
		throw new UnsupportedOperationException("Method not implemented");
	}

}
