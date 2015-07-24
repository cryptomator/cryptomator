package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.atomic.AtomicLong;

import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;

public class SamplingCryptorDecorator extends AbstractCryptorDecorator implements CryptorIOSampling {

	private final AtomicLong encryptedBytes;
	private final AtomicLong decryptedBytes;

	private SamplingCryptorDecorator(Cryptor cryptor) {
		super(cryptor);
		encryptedBytes = new AtomicLong();
		decryptedBytes = new AtomicLong();
	}

	public static Cryptor decorate(Cryptor cryptor) {
		return new SamplingCryptorDecorator(cryptor);
	}

	@Override
	public Long pollEncryptedBytes(boolean resetCounter) {
		if (resetCounter) {
			return encryptedBytes.getAndSet(0);
		} else {
			return encryptedBytes.get();
		}
	}

	@Override
	public Long pollDecryptedBytes(boolean resetCounter) {
		if (resetCounter) {
			return decryptedBytes.getAndSet(0);
		} else {
			return decryptedBytes.get();
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
		private final AtomicLong counter;

		private CountingInputStream(AtomicLong counter, InputStream in) {
			this.in = in;
			this.counter = counter;
		}

		@Override
		public int read() throws IOException {
			int count = in.read();
			counter.addAndGet(count);
			return count;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int count = in.read(b, off, len);
			counter.addAndGet(count);
			return count;
		}

	}

	private class CountingOutputStream extends OutputStream {

		private final OutputStream out;
		private final AtomicLong counter;

		private CountingOutputStream(AtomicLong counter, OutputStream out) {
			this.out = out;
			this.counter = counter;
		}

		@Override
		public void write(int b) throws IOException {
			counter.incrementAndGet();
			out.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			counter.addAndGet(len);
			out.write(b, off, len);
		}

	}

}
