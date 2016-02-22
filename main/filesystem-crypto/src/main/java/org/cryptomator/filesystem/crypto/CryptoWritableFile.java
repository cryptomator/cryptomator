/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class CryptoWritableFile implements WritableFile {

	final WritableFile file;
	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final FileContentCryptor cryptor;

	private FileContentEncryptor encryptor;
	private Future<Void> writeTask;

	public CryptoWritableFile(FileContentCryptor cryptor, WritableFile file) {
		this.file = file;
		this.cryptor = cryptor;
		initialize(0);
	}

	private void initialize(long firstCleartextByte) {
		encryptor = cryptor.createFileContentEncryptor(Optional.empty(), firstCleartextByte);
		writeHeader(); // write header with "zero content length" to avoid read access while still writing
		writeTask = executorService.submit(new CiphertextWriter(file, encryptor));
	}

	private void writeHeader() {
		ByteBuffer header = encryptor.getHeader();
		header.rewind();
		file.position(0);
		file.write(header);
	}

	@Override
	public int write(ByteBuffer source) {
		final int size = source.remaining();
		final ByteBuffer cleartextCopy = ByteBuffer.allocate(size);
		ByteBuffers.copy(source, cleartextCopy);
		cleartextCopy.flip();
		try {
			encryptor.append(cleartextCopy);
			return size;
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("Task interrupted while waiting for encryptor capacity"));
		}
	}

	@Override
	public void position(long position) {
		throw new UnsupportedOperationException("Partial write not implemented yet.");
	}

	@Override
	public void truncate() {
		terminateAndWaitForWriteTask();
		file.truncate();
		initialize(0);
	}

	@Override
	public boolean isOpen() {
		return file.isOpen();
	}

	@Override
	public void close() {
		try {
			if (file.isOpen()) {
				terminateAndWaitForWriteTask();
				writeHeader();
			}
		} finally {
			executorService.shutdownNow();
			file.close();
		}
	}

	private void terminateAndWaitForWriteTask() {
		try {
			encryptor.append(FileContentCryptor.EOF);
			writeTask.get();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof UncheckedIOException || e.getCause() instanceof IOException) {
				throw new UncheckedIOException(new IOException(e));
			} else {
				throw new IllegalStateException("Unexpected exception while waiting for encrypted file to be written", e);
			}
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("Task interrupted while flushing encrypted content"));
		}
	}

}
