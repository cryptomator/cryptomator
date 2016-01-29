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
			callInterruptibly();
		} catch (InterruptedException e) {
			throw new InterruptedIOException("Task interrupted while waiting for ciphertext");
		}
		return null;
	}

	private void callInterruptibly() throws InterruptedException {
		try {
			ByteBuffer ciphertext;
			while ((ciphertext = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				file.write(ciphertext);
			}
		} catch (UncheckedIOException e) {
			encryptor.cancelWithException(e);
		}
	}

}