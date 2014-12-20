package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.io.OutputStream;

class OffsetFilterOutputStream extends java.io.FilterOutputStream {

	private final long offset;
	private long bytesWritten;

	OffsetFilterOutputStream(OutputStream out, long offset) {
		super(out);
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must be greater than or equal 0.");
		}
		this.offset = offset;
	}

	@Override
	public void write(int b) throws IOException {
		this.write(new byte[] {(byte) b});
	}

	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		final long adjustedOffset = remainingOffset() + off;
		final long adjustedLength = len - remainingOffset();

		if (adjustedOffset < b.length && adjustedLength <= b.length) {
			// b.length is INT, so by definition adjustedOffset and adjustedLength must be INT too and we can safely cast:
			out.write(b, (int) adjustedOffset, (int) adjustedLength);
		}
		bytesWritten += len;
	}

	private long remainingOffset() {
		if (bytesWritten < offset) {
			return offset - bytesWritten;
		} else {
			return 0l;
		}
	}

}
