package org.cryptomator.common;

import javax.security.auth.Destroyable;
import java.util.Arrays;

/**
 * A destroyable CharSequence.
 */
public class Passphrase implements Destroyable, CharSequence {

	private final char[] data;
	private final int offset;
	private final int length;
	private boolean destroyed;

	/**
	 * Wraps (doesn't copy) the given data.
	 *
	 * @param data The wrapped data. Any changes to this will be reflected in this passphrase
	 */
	public Passphrase(char[] data) {
		this(data, 0, data.length);
	}

	/**
	 * Wraps (doesn't copy) a subarray of the given data.
	 *
	 * @param data The wrapped data. Any changes to this will be reflected in this passphrase
	 * @param offset The subarray offset, i.e. the first character of this passphrase
	 * @param length The subarray length, i.e. the length of this passphrase
	 */
	public Passphrase(char[] data, int offset, int length) {
		if (offset < 0 || length < 0 || offset + length > data.length) {
			throw new IndexOutOfBoundsException("[%1$d %1$d + %2$d[ not within [0, %3$d[".formatted(offset, length, data.length));
		}
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	public static Passphrase copyOf(CharSequence cs) {
		char[] result = new char[cs.length()];
		for (int i = 0; i < cs.length(); i++) {
			result[i] = cs.charAt(i);
		}
		return new Passphrase(result);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Passphrase that = (Passphrase) o;
		// time-constant comparison
		int diff = 0;
		for (int i = 0; i < length; i++) {
			diff |= charAt(i) ^ that.charAt(i);
		}
		return diff == 0;
	}

	@Override
	public int hashCode() {
		// basically Arrays.hashCode, but only for a certain subarray
		int result = 1;
		for (int i = 0; i < length; i++) {
			result = 31 * result + charAt(i);
		}
		return result;
	}

	@Override
	public String toString() {
		return new String(data, offset, length);
	}

	@Override
	public int length() {
		return length;
	}

	@Override
	public char charAt(int index) {
		if (index < 0 || index >= length) {
			throw new IndexOutOfBoundsException("%d not within [0, %d[".formatted(index, length));
		}
		return data[offset + index];
	}

	@Override
	public Passphrase subSequence(int start, int end) {
		if (start < 0 || end < 0 || end > length || start > end) {
			throw new IndexOutOfBoundsException("[%d, %d[ not within [0, %d[".formatted(start, end, length));
		}
		return new Passphrase(Arrays.copyOfRange(data, offset + start, offset + end));
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		Arrays.fill(data, offset, offset + length, '\0');
		destroyed = true;
	}
}
