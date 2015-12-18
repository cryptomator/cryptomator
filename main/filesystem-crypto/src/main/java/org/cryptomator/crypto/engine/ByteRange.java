package org.cryptomator.crypto.engine;

public class ByteRange {

	private final long start;
	private final long length;

	private ByteRange(long start, long length) {
		if (start < 0) {
			throw new IllegalArgumentException("start must not be a negative value");
		}
		if (length < 0) {
			throw new IllegalArgumentException("length must not be a negative value");
		}
		this.start = start;
		this.length = length;
	}

	static ByteRange of(long start, long length) {
		return new ByteRange(start, length);
	}

	/**
	 * @return Begin of range (inclusive)
	 */
	public long start() {
		return start;
	}

	/**
	 * @return End of range (exclusive)
	 */
	public long end() {
		return start + length;
	}

	/**
	 * @return Number of bytes between start and end
	 */
	public long length() {
		return length;
	}
}
