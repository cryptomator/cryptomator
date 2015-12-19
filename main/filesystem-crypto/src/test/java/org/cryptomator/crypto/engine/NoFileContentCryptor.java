package org.cryptomator.crypto.engine;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class NoFileContentCryptor implements FileContentCryptor {

	@Override
	public int getHeaderSize() {
		return Long.BYTES;
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header) {
		if (header.remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header size.");
		}
		return new Decryptor(header);
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header) {
		return new Encryptor();
	}

	private class Decryptor implements FileContentDecryptor {

		private final BlockingQueue<ByteBuffer> cleartextQueue = new LinkedBlockingQueue<>();
		private final long contentLength;

		private Decryptor(ByteBuffer header) {
			assert header.remaining() == Long.BYTES;
			this.contentLength = header.getLong();
		}

		@Override
		public long contentLength() {
			return contentLength;
		}

		@Override
		public void append(ByteBuffer ciphertext) {
			try {
				if (ciphertext == FileContentDecryptor.EOF) {
					cleartextQueue.put(FileContentDecryptor.EOF);
				} else {
					cleartextQueue.put(ciphertext.asReadOnlyBuffer());
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public BlockingQueue<ByteBuffer> cleartext() {
			return cleartextQueue;
		}

		@Override
		public ByteRange ciphertextRequiredToDecryptRange(ByteRange cleartextRange) {
			return cleartextRange;
		}

		@Override
		public void skipToPosition(long nextCiphertextByte) throws IllegalArgumentException {
			// no-op
		}

	}

	private class Encryptor implements FileContentEncryptor {

		private final BlockingQueue<ByteBuffer> ciphertextQueue = new LinkedBlockingQueue<>();
		private long numCleartextBytesEncrypted = 0;

		@Override
		public ByteBuffer getHeader() {
			ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
			buf.putLong(numCleartextBytesEncrypted);
			return buf;
		}

		@Override
		public void append(ByteBuffer cleartext) {
			try {
				if (cleartext == FileContentEncryptor.EOF) {
					ciphertextQueue.put(FileContentEncryptor.EOF);
				} else {
					int cleartextLen = cleartext.remaining();
					ciphertextQueue.put(cleartext.asReadOnlyBuffer());
					numCleartextBytesEncrypted += cleartextLen;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public BlockingQueue<ByteBuffer> ciphertext() {
			return ciphertextQueue;
		}

		@Override
		public ByteRange cleartextRequiredToEncryptRange(ByteRange cleartextRange) {
			return cleartextRange;
		}

		@Override
		public void skipToPosition(long nextCleartextByte) throws IllegalArgumentException {
			// no-op
		}

	}

}
