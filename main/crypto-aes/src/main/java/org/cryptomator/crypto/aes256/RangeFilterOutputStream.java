package org.cryptomator.crypto.aes256;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Passthrough of all bytes except for certain bytes at the begin and end of the stream, which will get cut off.
 */
class RangeFilterOutputStream extends FilterOutputStream {

	RangeFilterOutputStream(OutputStream out, long offset, long limit) {
		super(new OffsetFilterOutputStream(new LimitFilterOutputStream(out, limit), offset));
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		out.write(b, off, len);
	}

}
