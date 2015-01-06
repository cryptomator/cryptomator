package org.cryptomator.crypto.aes256;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Mac;

/**
 * Updates a {@link Mac} with the bytes read from this stream.
 */
class MacInputStream extends FilterInputStream {

	private final Mac mac;

	/**
	 * @param in Stream from which to read contents, which will update the Mac.
	 * @param mac Mac to be updated during writes.
	 */
	public MacInputStream(InputStream in, Mac mac) {
		super(in);
		this.mac = mac;
	}

	@Override
	public int read() throws IOException {
		int b = in.read();
		mac.update((byte) b);
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		mac.update(b);
		return read;
	}

}
