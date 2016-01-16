/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class CryptoReadableFile implements ReadableFile {

	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final ByteBuffer header;
	private final FileContentCryptor cryptor;
	private final ReadableFile file;
	private FileContentDecryptor decryptor;
	private Future<Void> readAheadTask;
	private ByteBuffer bufferedCleartext = EMPTY_BUFFER;

	public CryptoReadableFile(FileContentCryptor cryptor, ReadableFile file) {
		this.header = ByteBuffer.allocate(cryptor.getHeaderSize());
		this.cryptor = cryptor;
		this.file = file;
		file.position(0);
		file.read(header);
		header.flip();
		this.position(0);
	}

	@Override
	public int read(ByteBuffer target) {
		try {
			if (bufferedCleartext == FileContentCryptor.EOF) {
				return -1;
			}
			int bytesRead = 0;
			while (target.remaining() > 0 && bufferedCleartext != FileContentCryptor.EOF) {
				bufferCleartext();
				bytesRead += readFromBufferedCleartext(target);
			}
			return bytesRead;
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("Task interrupted while waiting for cleartext"));
		}
	}

	@Override
	public long size() throws UncheckedIOException {
		assert decryptor != null : "decryptor is always being set during position(long)";
		return decryptor.contentLength();
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		if (readAheadTask != null) {
			readAheadTask.cancel(true);
			bufferedCleartext = EMPTY_BUFFER;
		}
		long ciphertextPos = cryptor.toCiphertextPos(position);
		decryptor = cryptor.createFileContentDecryptor(header.asReadOnlyBuffer(), ciphertextPos);
		readAheadTask = executorService.submit(new CiphertextReader(file, decryptor, header.remaining() + ciphertextPos));
	}

	private void bufferCleartext() throws InterruptedException {
		if (!bufferedCleartext.hasRemaining()) {
			bufferedCleartext = decryptor.cleartext();
		}
	}

	private int readFromBufferedCleartext(ByteBuffer target) {
		assert bufferedCleartext != null;
		return ByteBuffers.copy(bufferedCleartext, target);
	}

	@Override
	public void copyTo(WritableFile other) {
		if (other instanceof CryptoWritableFile) {
			CryptoWritableFile dst = (CryptoWritableFile) other;
			file.copyTo(dst.file);
		} else {
			throw new IllegalArgumentException("Can not move CryptoFile to conventional File.");
		}
	}

	@Override
	public boolean isOpen() {
		return file.isOpen();
	}

	@Override
	public void close() {
		executorService.shutdownNow();
		file.close();
	}

}
