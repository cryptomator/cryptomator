package org.cryptomator.filesystem.crypto;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.filesystem.ReadableFile;

class CiphertextReader implements Callable<Void> {

	private static final int READ_BUFFER_SIZE = 32 * 1024 + 32; // aligned with encrypted chunk size + MAC size

	private final ReadableFile file;
	private final FileContentDecryptor decryptor;
	private final long startpos;

	public CiphertextReader(ReadableFile file, FileContentDecryptor decryptor, long startpos) {
		this.file = file;
		this.decryptor = decryptor;
		this.startpos = startpos;
	}

	@Override
	public Void call() {
		file.position(startpos);
		int bytesRead = -1;
		try {
			do {
				ByteBuffer ciphertext = ByteBuffer.allocate(READ_BUFFER_SIZE);
				file.read(ciphertext);
				ciphertext.flip();
				bytesRead = ciphertext.remaining();
				if (bytesRead > 0) {
					decryptor.append(ciphertext);
				}
			} while (bytesRead > 0);
			decryptor.append(FileContentCryptor.EOF);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

}