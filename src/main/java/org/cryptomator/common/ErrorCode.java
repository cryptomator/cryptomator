package org.cryptomator.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Locale;

public class ErrorCode {

	private final static int A_PRIME = Integer.MAX_VALUE;
	private final static String DELIM = ":";

	private final static int LATEST_FRAME = 1;
	private final static int ALL_FRAMES = Integer.MAX_VALUE;

	private final Throwable origCause;
	private final Throwable rootCause;

	// visible for testing
	ErrorCode(Throwable cause) {
		Preconditions.checkNotNull(cause);
		this.origCause = cause;
		this.rootCause = rootCause(cause);
	}

	// visible for testing
	String methodCode() {
		return format(traceCode(rootCause, LATEST_FRAME));
	}

	// visible for testing
	String rootCauseCode() {
		return format(traceCode(rootCause, ALL_FRAMES));
	}

	// visible for testing
	String origCauseCode() {
		return format(traceCode(origCause, ALL_FRAMES));
	}

	@Override
	public String toString() {
		return methodCode() + DELIM + rootCauseCode() + DELIM + origCauseCode();
	}

	/**
	 * Deterministically creates an error code from the stack trace of the given <code>cause</code>.
	 * <p>
	 * The code consists of three parts separated by {@value DELIM}:
	 * <ul>
	 *     <li>The first part depends on the root cause and the method that threw it</li>
	 *     <li>The second part depends on the root cause and its stack trace</li>
	 *     <li>The third part depends on all the cause hierarchy</li>
	 * </ul>
	 * <p>
	 * Parts may be identical if the cause is the root cause or the root cause has just one single item in its stack trace.
	 *
	 * @param cause The exception
	 * @return A three-part error code
	 */
	public static String of(Throwable cause) {
		return new ErrorCode(cause).toString();
	}

	private Throwable rootCause(Throwable e) {
		if (e.getCause() == null) {
			return e;
		}
		return rootCause(e.getCause());
	}

	private String format(int value) {
		//Cut off highest 12 bits (only leave 20 least significant bits) and XOR rest with cutoff
		value = (value & 0xfffff) ^ (value >>> 20);
		return Strings.padStart(Integer.toString(value, 32).toUpperCase(Locale.ROOT), 4, '0');
	}

	private int traceCode(Throwable e, int frameCount) {
		int result = 0x6c528c4a;
		if (e.getCause() != null) {
			result = traceCode(e.getCause(), frameCount);
		}
		result = result * A_PRIME + e.getClass().getName().hashCode();
		var stack = e.getStackTrace();
		for (int i = 0; i < Math.min(stack.length, frameCount); i++) {
			result = result * A_PRIME + stack[i].getClassName().hashCode();
			result = result * A_PRIME + stack[i].getMethodName().hashCode();
		}
		return result;
	}
}