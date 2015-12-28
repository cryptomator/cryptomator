package org.cryptomator.crypto.fs;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;
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
	private final FileContentEncryptor encryptor;
	private final Future<Void> writeTask;

	public CryptoWritableFile(FileContentCryptor cryptor, WritableFile file) {
		this.file = file;
		this.encryptor = cryptor.createFileContentEncryptor(Optional.empty());
		writeHeader();
		this.writeTask = executorService.submit(new Writer());
	}

	private void writeHeader() {
		ByteBuffer header = encryptor.getHeader();
		header.rewind();
		file.write(header, 0);
	}

	@Override
	public void write(ByteBuffer source) {
		final ByteBuffer cleartextCopy = ByteBuffer.allocate(source.remaining());
		ByteBuffers.copy(source, cleartextCopy);
		cleartextCopy.flip();
		try {
			encryptor.append(cleartextCopy);
			file.write(source);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void write(ByteBuffer source, int position) {
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
		this.write(ByteBuffer.allocate(0), 0);
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

	private class Writer implements Callable<Void> {

		@Override
		public Void call() {
			try {
				ByteBuffer ciphertext;
				while ((ciphertext = encryptor.ciphertext()) != FileContentCryptor.EOF) {
					file.write(ciphertext);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return null;
		}

	}

}
