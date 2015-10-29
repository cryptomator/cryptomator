package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.atomic.LongAdder;

import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;

/**
 * Decorates the Cryptor by decorating the In- and OutputStreams used during de-/encryption.
 */
public class SamplingCryptorDecorator extends AbstractCryptorDecorator implements CryptorIOSampling {

	private final LongAdder encryptedBytes;
	private final LongAdder decryptedBytes;

	private SamplingCryptorDecorator(Cryptor cryptor) {
		super(cryptor);
		encryptedBytes = new LongAdder();
		decryptedBytes = new LongAdder();
	}

	public static Cryptor decorate(Cryptor cryptor) {
		return new SamplingCryptorDecorator(cryptor);
	}

	@Override
	public long pollEncryptedBytes(boolean resetCounter) {
		if (resetCounter) {
			return encryptedBytes.sumThenReset();
		} else {
			return encryptedBytes.sum();
		}
	}

	@Override
	public long pollDecryptedBytes(boolean resetCounter) {
		if (resetCounter) {
			return decryptedBytes.sumThenReset();
		} else {
			return decryptedBytes.sum();
		}
	}

	/* Cryptor */

	@Override
	public Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile, boolean authenticate) throws IOException, DecryptFailedException {
		final OutputStream countingOutputStream = new CountingOutputStream(decryptedBytes, plaintextFile);
		return cryptor.decryptFile(encryptedFile, countingOutputStream, authenticate);
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length, boolean authenticate) throws IOException, DecryptFailedException {
		final OutputStream countingOutputStream = new CountingOutputStream(decryptedBytes, plaintextFile);
		return cryptor.decryptRange(encryptedFile, countingOutputStream, pos, length, authenticate);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException {
		final InputStream countingInputStream = new CountingInputStream(encryptedBytes, plaintextFile);
		return cryptor.encryptFile(countingInputStream, encryptedFile);
	}

	private class CountingInputStream extends InputStream {

		private final InputStream in;
		private final LongAdder counter;

		private CountingInputStream(LongAdder counter, InputStream in) {
			this.in = in;
			this.counter = counter;
		}

		@Override
		public int read() throws IOException {
			int count = in.read();
			counter.add(count);
			return count;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int count = in.read(b, off, len);
			counter.add(count);
			return count;
		}

	}

	private class CountingOutputStream extends OutputStream {

		private final OutputStream out;
		private final LongAdder counter;

		private CountingOutputStream(LongAdder counter, OutputStream out) {
			this.out = out;
			this.counter = counter;
		}

		@Override
		public void write(int b) throws IOException {
			counter.increment();
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			counter.add(len);
			out.write(b, off, len);
		}

	}

}
