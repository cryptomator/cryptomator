package org.cryptomator.crypto.aes256;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throws an exception, if more than (2^32)-1 16 byte blocks will be encrypted (would result in an counter overflow).<br/>
 * From https://tools.ietf.org/html/rfc3686: <cite> Using the encryption process described in section 2.1, this construction permits each packet to consist of up to: (2^32)-1 blocks</cite>
 */
class CounterAwareInputStream extends FilterInputStream {

	static final long SIXTY_FOUR_GIGABYE = ((1l << 32) - 1) * 16;

	private final AtomicLong counter;

	/**
	 * @param in Stream from which to read contents, which will update the Mac.
	 */
	public CounterAwareInputStream(InputStream in) {
		super(in);
		this.counter = new AtomicLong(0l);
	}

	@Override
	public int read() throws IOException {
		int b = in.read();
		if (b != -1) {
			final long currentValue = counter.incrementAndGet();
			failWhen64GibReached(currentValue);
		}
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read > 0) {
			final long currentValue = counter.addAndGet(read);
			failWhen64GibReached(currentValue);
		}
		return read;
	}

	private void failWhen64GibReached(long currentValue) throws CounterAwareInputLimitReachedException {
		if (currentValue > SIXTY_FOUR_GIGABYE) {
			throw new CounterAwareInputLimitReachedException();
		}
	}

	static class CounterAwareInputLimitReachedException extends IOException {
		private static final long serialVersionUID = -1905012809288019359L;

	}

}
