package org.cryptomator.crypto.aes256;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Mac;

/**
 * Updates a {@link Mac} with the bytes written to this stream.
 */
class MacOutputStream extends FilterOutputStream {

	private final Mac mac;

	/**
	 * @param out Stream to redirect contents to after updating the mac.
	 * @param mac Mac to be updated during writes.
	 */
	public MacOutputStream(OutputStream out, Mac mac) {
		super(out);
		this.mac = mac;
	}

	@Override
	public void write(int b) throws IOException {
		mac.update((byte) b);
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		mac.update(b, off, len);
		out.write(b, off, len);
	}

}
