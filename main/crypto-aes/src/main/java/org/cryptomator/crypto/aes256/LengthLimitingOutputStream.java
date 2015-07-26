package org.cryptomator.crypto.aes256;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LengthLimitingOutputStream extends FilterOutputStream {

	private final long limit;
	private volatile long bytesWritten;

	public LengthLimitingOutputStream(OutputStream out, long limit) {
		super(out);
		this.limit = limit;
		this.bytesWritten = 0;
	}

	@Override
	public void write(int b) throws IOException {
		if (bytesWritten < limit) {
			out.write(b);
			bytesWritten++;
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		final long bytesAvailable = limit - bytesWritten;
		final int adjustedLen = (int) Math.min(len, bytesAvailable);
		if (adjustedLen > 0) {
			out.write(b, off, adjustedLen);
			bytesWritten += adjustedLen;
		}
	}

	public long getBytesWritten() {
		return bytesWritten;
	}

}
