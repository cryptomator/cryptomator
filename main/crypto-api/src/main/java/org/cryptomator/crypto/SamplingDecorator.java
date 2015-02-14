package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;

public class SamplingDecorator implements Cryptor, CryptorIOSampling {

	private final Cryptor cryptor;
	private final AtomicLong encryptedBytes;
	private final AtomicLong decryptedBytes;

	private SamplingDecorator(Cryptor cryptor) {
		this.cryptor = cryptor;
		encryptedBytes = new AtomicLong();
		decryptedBytes = new AtomicLong();
	}

	public static Cryptor decorate(Cryptor cryptor) {
		return new SamplingDecorator(cryptor);
	}

	@Override
	public void swipeSensitiveData() {
		cryptor.swipeSensitiveData();
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
	public void encryptMasterKey(OutputStream out, CharSequence password) throws IOException {
		cryptor.encryptMasterKey(out, password);
	}

	@Override
	public void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException {
		cryptor.decryptMasterKey(in, password);
	}

	@Override
	public String encryptPath(String cleartextPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport) {
		encryptedBytes.addAndGet(StringUtils.length(cleartextPath));
		return cryptor.encryptPath(cleartextPath, encryptedPathSep, cleartextPathSep, ioSupport);
	}

	@Override
	public String decryptPath(String encryptedPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport) throws DecryptFailedException {
		decryptedBytes.addAndGet(StringUtils.length(encryptedPath));
		return cryptor.decryptPath(encryptedPath, encryptedPathSep, cleartextPathSep, ioSupport);
	}

	@Override
	public boolean authenticateContent(SeekableByteChannel encryptedFile) throws IOException {
		return cryptor.authenticateContent(encryptedFile);
	}

	@Override
	public Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException {
		return cryptor.decryptedContentLength(encryptedFile);
	}

	@Override
	public Long decryptedFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException, DecryptFailedException {
		final OutputStream countingInputStream = new CountingOutputStream(decryptedBytes, plaintextFile);
		return cryptor.decryptedFile(encryptedFile, countingInputStream);
	}

	@Override
	public Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException, DecryptFailedException {
		final OutputStream countingInputStream = new CountingOutputStream(decryptedBytes, plaintextFile);
		return cryptor.decryptRange(encryptedFile, countingInputStream, pos, length);
	}

	@Override
	public Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException {
		final InputStream countingInputStream = new CountingInputStream(encryptedBytes, plaintextFile);
		return cryptor.encryptFile(countingInputStream, encryptedFile);
	}

	@Override
	public Filter<Path> getPayloadFilesFilter() {
		return cryptor.getPayloadFilesFilter();
	}

	@Override
	public void addSensitiveDataSwipeListener(SensitiveDataSwipeListener listener) {
		cryptor.addSensitiveDataSwipeListener(listener);
	}

	@Override
	public void removeSensitiveDataSwipeListener(SensitiveDataSwipeListener listener) {
		cryptor.removeSensitiveDataSwipeListener(listener);
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
