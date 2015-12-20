package org.cryptomator.crypto.fs;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class CryptoReadableFile implements ReadableFile {

	private static final int READ_BUFFER_SIZE = 32 * 1024 + 32; // aligned with encrypted chunk size + MAC size
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final FileContentDecryptor decryptor;
	private final ReadableFile file;
	private Future<Void> readAheadTask;
	private ByteBuffer bufferedCleartext = EMPTY_BUFFER;

	public CryptoReadableFile(FileContentCryptor cryptor, ReadableFile file) {
		final int headerSize = cryptor.getHeaderSize();
		final ByteBuffer header = ByteBuffer.allocate(headerSize);
		file.read(header, 0);
		header.flip();
		this.decryptor = cryptor.createFileContentDecryptor(header);
		this.file = file;
		this.prepareReadAtPhysicalPosition(headerSize + 0);
	}

	private void prepareReadAtPhysicalPosition(long pos) {
		if (readAheadTask != null) {
			readAheadTask.cancel(true);
			bufferedCleartext = EMPTY_BUFFER;
		}
		readAheadTask = executorService.submit(new Reader(pos));
	}

	@Override
	public void read(ByteBuffer target) {
		try {
			while (target.remaining() > 0 && bufferedCleartext != FileContentCryptor.EOF) {
				bufferCleartext();
				readFromBufferedCleartext(target);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void bufferCleartext() throws InterruptedException {
		if (!bufferedCleartext.hasRemaining()) {
			bufferedCleartext = decryptor.cleartext();
		}
	}

	private void readFromBufferedCleartext(ByteBuffer target) {
		assert bufferedCleartext != null;
		ByteBuffers.copy(bufferedCleartext, target);
	}

	@Override
	public void read(ByteBuffer target, long position) {
		throw new UnsupportedOperationException("Partial read not implemented yet.");
	}

	@Override
	public void copyTo(WritableFile other) {
		file.copyTo(other);
	}

	@Override
	public void close() {
		executorService.shutdownNow();
		file.close();
	}

	private class Reader implements Callable<Void> {

		private final long startpos;

		public Reader(long startpos) {
			this.startpos = startpos;
		}

		@Override
		public Void call() {
			file.read(EMPTY_BUFFER, startpos);
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

}
