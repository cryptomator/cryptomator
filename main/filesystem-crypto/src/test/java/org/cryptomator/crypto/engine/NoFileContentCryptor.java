/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

class NoFileContentCryptor implements FileContentCryptor {

	@Override
	public int getHeaderSize() {
		return Long.BYTES;
	}

	@Override
	public long toCiphertextPos(long cleartextPos) {
		return cleartextPos;
	}

	@Override
	public FileContentDecryptor createFileContentDecryptor(ByteBuffer header, long firstCiphertextByte, boolean authenticate) {
		if (header.remaining() != getHeaderSize()) {
			throw new IllegalArgumentException("Invalid header size.");
		}
		return new Decryptor(header);
	}

	@Override
	public FileContentEncryptor createFileContentEncryptor(Optional<ByteBuffer> header, long firstCleartextByte) {
		return new Encryptor();
	}

	private class Decryptor implements FileContentDecryptor {

		private final BlockingQueue<Supplier<ByteBuffer>> cleartextQueue = new LinkedBlockingQueue<>();
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
				if (ciphertext == FileContentCryptor.EOF) {
					cleartextQueue.put(() -> FileContentCryptor.EOF);
				} else {
					cleartextQueue.put(ciphertext::asReadOnlyBuffer);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void cancelWithException(Exception cause) throws InterruptedException {
			cleartextQueue.put(() -> {
				throw new UncheckedIOException(new IOException(cause));
			});
		}

		@Override
		public ByteBuffer cleartext() throws InterruptedException {
			return cleartextQueue.take().get();
		}

		@Override
		public void destroy() {
			// no-op
		}

	}

	private class Encryptor implements FileContentEncryptor {

		private final BlockingQueue<Supplier<ByteBuffer>> ciphertextQueue = new LinkedBlockingQueue<>();
		private long numCleartextBytesEncrypted = 0;

		@Override
		public ByteBuffer getHeader() {
			ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
			buf.putLong(numCleartextBytesEncrypted);
			return buf;
		}

		@Override
		public int getHeaderSize() {
			return Long.BYTES;
		}

		@Override
		public void append(ByteBuffer cleartext) {
			try {
				if (cleartext == FileContentCryptor.EOF) {
					ciphertextQueue.put(() -> FileContentCryptor.EOF);
				} else {
					int cleartextLen = cleartext.remaining();
					ciphertextQueue.put(cleartext::asReadOnlyBuffer);
					numCleartextBytesEncrypted += cleartextLen;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void cancelWithException(Exception cause) throws InterruptedException {
			ciphertextQueue.put(() -> {
				throw new UncheckedIOException(new IOException(cause));
			});
		}

		@Override
		public ByteBuffer ciphertext() throws InterruptedException {
			return ciphertextQueue.take().get();
		}

		@Override
		public void destroy() {
			// no-op
		}

	}

}
