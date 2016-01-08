/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cryptomator.common.UncheckedInterruptedException;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class CryptoWritableFile implements WritableFile {

	final WritableFile file;
	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final FileContentEncryptor encryptor;
	private final Future<Void> writeTask;

	public CryptoWritableFile(FileContentCryptor cryptor, WritableFile file) {
		this.file = file;
		this.encryptor = cryptor.createFileContentEncryptor(Optional.empty(), 0);
		writeHeader();
		this.writeTask = executorService.submit(new CiphertextWriter(file, encryptor));
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
			Thread.currentThread().interrupt();
			throw new UncheckedInterruptedException(e);
		}
	}

	@Override
	public void position(long position) {
		throw new UnsupportedOperationException("Partial write not implemented yet.");
	}

	@Override
	public void moveTo(WritableFile other) {
		if (other instanceof CryptoWritableFile) {
			CryptoWritableFile dst = (CryptoWritableFile) other;
			file.moveTo(dst.file);
		} else {
			throw new IllegalArgumentException("Can not move CryptoFile to conventional File.");
		}
	}

	@Override
	public void setLastModified(Instant instant) {
		file.setLastModified(instant);
	}

	@Override
	public void delete() {
		file.delete();
	}

	@Override
	public void truncate() {
		/*
		 * TODO kill writer thread (EOF) and reinitialize CryptoWritableFile
		 * after truncating the file
		 */
		throw new UnsupportedOperationException("Truncate not supported yet");
	}

	@Override
	public void setCreationTime(Instant instant) throws UncheckedIOException {
		file.setCreationTime(instant);
	}

	@Override
	public boolean isOpen() {
		return file.isOpen();
	}

	@Override
	public void close() {
		try {
			encryptor.append(FileContentCryptor.EOF);
			writeTask.get();
			writeHeader();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof UncheckedIOException) {
				throw (UncheckedIOException) e.getCause();
			} else {
				throw new IllegalStateException("Unexpected exception while waiting for encrypted file to be written", e);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			executorService.shutdownNow();
			file.close();
		}
	}

}
