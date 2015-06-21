package org.cryptomator.crypto.aes256;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Not thread-safe!
 */
public class LengthObfuscationInputStream extends FilterInputStream {

	private final byte[] padding;
	private int paddingLength = -1;
	private long inputBytesRead = 0;
	private int paddingBytesRead = 0;

	LengthObfuscationInputStream(InputStream in, byte[] padding) {
		super(in);
		this.padding = padding;
	}

	long getRealInputLength() {
		return inputBytesRead;
	}

	private void choosePaddingLengthOnce() {
		if (paddingLength == -1) {
			long upperBound = Math.min(inputBytesRead / 10, 16 * 1024 * 1024); // 10% of original bytes, but not more than 16MiBs
			paddingLength = (int) (Math.random() * upperBound);
		}
	}

	@Override
	public int read() throws IOException {
		final int b = in.read();
		if (b != -1) {
			// stream available:
			inputBytesRead++;
			return b;
		} else {
			choosePaddingLengthOnce();
			return readFromPadding();
		}
	}

	private int readFromPadding() {
		if (paddingLength == -1) {
			throw new IllegalStateException("No padding length chosen yet.");
		}

		if (paddingBytesRead < paddingLength) {
			// padding available:
			return padding[paddingBytesRead++ % padding.length];
		} else {
			// end of stream AND padding
			return -1;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		final int n = in.read(b, 0, len);
		final int bytesRead = Math.max(0, n); // EOF -> 0
		inputBytesRead += bytesRead;

		if (bytesRead == len) {
			return bytesRead;
		} else if (bytesRead < len) {
			choosePaddingLengthOnce();
			final int additionalBytesNeeded = len - bytesRead;
			final int m = readFromPadding(b, bytesRead, additionalBytesNeeded);
			final int additionalBytesRead = Math.max(0, m); // EOF -> 0
			return (n == -1 && m == -1) ? -1 : bytesRead + additionalBytesRead;
		} else {
			// bytesRead > len:
			throw new IllegalStateException("read more bytes than requested.");
		}
	}

	private int readFromPadding(byte[] b, int off, int len) {
		if (paddingLength == -1) {
			throw new IllegalStateException("No padding length chosen yet.");
		}

		final int remainingPadding = paddingLength - paddingBytesRead;
		if (remainingPadding > len) {
			// padding available:
			for (int i = 0; i < len; i++) {
				b[off + i] = padding[paddingBytesRead + i % padding.length];
			}
			paddingBytesRead += len;
			return len;
		} else if (remainingPadding > 0) {
			// partly available:
			for (int i = 0; i < remainingPadding; i++) {
				b[off + i] = padding[paddingBytesRead + i % padding.length];
			}
			paddingBytesRead += remainingPadding;
			return remainingPadding;
		} else {
			// end of stream AND padding
			return -1;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		throw new IOException("Skip not supported");
	}

	@Override
	public int available() throws IOException {
		final int inputAvailable = in.available();
		if (inputAvailable > 0) {
			return inputAvailable;
		} else {
			// remaining padding
			choosePaddingLengthOnce();
			return paddingLength - paddingBytesRead;
		}
	}

	@Override
	public boolean markSupported() {
		return false;
	}

}
