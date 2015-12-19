package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;

class FileContentCryptorImpl implements FileContentCryptor {

	// 16 header IV, 8 content nonce, 48 sensitive header data, 32 headerMac
	static final int HEADER_SIZE = 104;

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
		return HEADER_SIZE;
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header) {
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header) {
		return new FileContentEncryptorImpl(encryptionKey, macKey, randomSource);
	}

}
