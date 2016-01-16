package org.cryptomator.filesystem.crypto;

import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.filesystem.WritableFile;

class CiphertextWriter implements Callable<Void> {

	private final WritableFile file;
	private final FileContentEncryptor encryptor;

	public CiphertextWriter(WritableFile file, FileContentEncryptor encryptor) {
		this.file = file;
		this.encryptor = encryptor;
	}

	@Override
	public Void call() throws InterruptedIOException {
		try {
			ByteBuffer ciphertext;
			while ((ciphertext = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				file.write(ciphertext);
			}
		} catch (InterruptedException e) {
			throw new InterruptedIOException("Task interrupted while waiting for ciphertext");
		}
		return null;
	}

}