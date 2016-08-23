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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cryptomator.crypto.engine.AuthenticationFailedException;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.io.ByteBuffers;

class CryptoReadableFile implements ReadableFile {

	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final ByteBuffer header;
	private final FileContentCryptor cryptor;
	private final ReadableFile file;
	private final boolean authenticate;
	private final Runnable onAuthError;
	private FileContentDecryptor decryptor;
	private Future<Void> readAheadTask;
	private ByteBuffer bufferedCleartext = EMPTY_BUFFER;

	public CryptoReadableFile(FileContentCryptor cryptor, ReadableFile file, boolean authenticate, Runnable onAuthError) {
		this.header = ByteBuffer.allocate(cryptor.getHeaderSize());
		this.cryptor = cryptor;
		this.file = file;
		this.authenticate = authenticate;
		this.onAuthError = onAuthError;
		file.position(0);
		int headerBytesRead = file.read(header);
		if (headerBytesRead != header.capacity()) {
			throw new IllegalArgumentException("File too short to contain a header.");
		}
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public long size() throws UncheckedIOException {
		long ciphertextSize = file.size() - cryptor.getHeaderSize();
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

	@Override
	public void position(long position) throws UncheckedIOException {
		if (readAheadTask != null) {
			readAheadTask.cancel(true);
			bufferedCleartext = EMPTY_BUFFER;
		}
		long ciphertextPos = cryptor.toCiphertextPos(position);
		decryptor = cryptor.createFileContentDecryptor(header.asReadOnlyBuffer(), ciphertextPos, authenticate);
		readAheadTask = executorService.submit(new CiphertextReader(file, decryptor, header.remaining() + ciphertextPos));
	}

	private void bufferCleartext() throws InterruptedException, IOException {
		if (!bufferedCleartext.hasRemaining()) {
			try {
				bufferedCleartext = decryptor.cleartext();
			} catch (AuthenticationFailedException e) {
				onAuthError.run();
				throw new IOException("Failed to decrypt file due to an authentication error.", e);
			}
		}
	}

	private int readFromBufferedCleartext(ByteBuffer target) {
		assert bufferedCleartext != null;
		return ByteBuffers.copy(bufferedCleartext, target);
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
