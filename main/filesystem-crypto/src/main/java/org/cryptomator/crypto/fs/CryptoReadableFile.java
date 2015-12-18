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

	private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private final FileContentDecryptor decryptor;
	private final ReadableFile file;
	private Future<Void> readAheadTask;
	private ByteBuffer bufferedCleartext;

	public CryptoReadableFile(FileContentCryptor cryptor, ReadableFile file) {
		final ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		file.read(header, 0);
		header.flip();
		this.decryptor = cryptor.getFileContentDecryptor(header);
		this.file = file;
		this.prepareReadAtPosition(0);
	}

	private void prepareReadAtPosition(long pos) {
		if (readAheadTask != null) {
			readAheadTask.cancel(true);
		}
		readAheadTask = executorService.submit(new Reader());
	}

	@Override
	public void read(ByteBuffer target) {
		try {
			while (target.remaining() > 0 && bufferedCleartext != FileContentDecryptor.EOF) {
				bufferCleartext();
				readFromBufferedCleartext(target);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			executorService.shutdownNow();
		}
	}

	private void bufferCleartext() throws InterruptedException {
		if (bufferedCleartext == null || !bufferedCleartext.hasRemaining()) {
			bufferedCleartext = decryptor.cleartext().take();
		}
	}

	private void readFromBufferedCleartext(ByteBuffer target) {
		assert bufferedCleartext != null;
		ByteBuffers.copy(bufferedCleartext, target);
	}

	@Override
	public void read(ByteBuffer target, int position) {
		throw new UnsupportedOperationException("Partial read not implemented yet.");
	}

	@Override
	public void copyTo(WritableFile other) {
		file.copyTo(other);
	}

	@Override
	public void close() {
		file.close();
	}

	private class Reader implements Callable<Void> {

		@Override
		public Void call() {
			int bytesRead = -1;
			do {
				ByteBuffer ciphertext = ByteBuffer.allocate(READ_BUFFER_SIZE);
				file.read(ciphertext);
				ciphertext.flip();
				bytesRead = ciphertext.remaining();
				if (bytesRead > 0) {
					decryptor.append(ciphertext);
				}
			} while (bytesRead > 0);
			decryptor.append(FileContentDecryptor.EOF);
			return null;
		}

	}

}
