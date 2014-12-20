package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.io.OutputStream;

class LimitFilterOutputStream extends java.io.FilterOutputStream {

	private final long limit;
	private long bytesWritten;

	LimitFilterOutputStream(OutputStream out, long limit) {
		super(out);
		if (limit < 0) {
			throw new IllegalArgumentException("Limit must be greater than or equal 0.");
		}
		this.limit = limit;
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
		final long adjustedLength = Math.min(bytesRemainingUntilReachingLimit(), len);

		// adjustedLength is <= len, so it must be INT and we can safely cast:
		out.write(b, off, (int) adjustedLength);
		bytesWritten += adjustedLength;
	}

	private long bytesRemainingUntilReachingLimit() {
		if (bytesWritten < limit) {
			return limit - bytesWritten;
		} else {
			return 0l;
		}
	}

}
