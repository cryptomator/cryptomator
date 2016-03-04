/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
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
	public Void call() throws InterruptedIOException {
		try {
			callInterruptibly();
		} catch (InterruptedException e) {
			throw new InterruptedIOException("Task interrupted while waiting for ciphertext");
		}
		return null;
	}

	private void callInterruptibly() throws InterruptedException {
		try {
			file.position(startpos);
			int bytesRead = -1;
			do {
				ByteBuffer ciphertext = ByteBuffer.allocate(READ_BUFFER_SIZE);
				bytesRead = file.read(ciphertext);
				if (bytesRead > 0) {
					ciphertext.flip();
					assert bytesRead == ciphertext.remaining();
					decryptor.append(ciphertext);
				}
			} while (bytesRead > 0);
			decryptor.append(FileContentCryptor.EOF);
		} catch (UncheckedIOException e) {
			decryptor.cancelWithException(e);
		}
	}

}