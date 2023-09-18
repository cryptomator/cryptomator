package org.cryptomator.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Holds a throwable and provides a human-readable {@link #toString() three-component string representation}
 * aiming to allow documentation and lookup of same or similar errors.
 */
public class ErrorCode {

	private final static int A_PRIME = 31;
	private final static int SEED = 0xdeadbeef;
	public final static String DELIM = ":";

	private final static int LATEST_FRAME = 1;
	private final static int ALL_FRAMES = Integer.MAX_VALUE;

	private final Throwable throwable;
	private final Throwable rootCause;
	private final int rootCauseSpecificFrames;

	private ErrorCode(Throwable throwable, Throwable rootCause, int rootCauseSpecificFrames) {
		this.throwable = Objects.requireNonNull(throwable);
		this.rootCause = Objects.requireNonNull(rootCause);
		this.rootCauseSpecificFrames = rootCauseSpecificFrames;
	}

	public String methodCode() {
		return format(traceCode(rootCause, LATEST_FRAME));
	}

	public String rootCauseCode() {
		return format(traceCode(rootCause, rootCauseSpecificFrames));
	}

	public String throwableCode() {
		return format(traceCode(throwable, ALL_FRAMES));
	}

	/**
	 * Produces an error code consisting of three {@value DELIM}-separated components.
	 * <p>
	 * A full match of the error code indicates the exact same throwable (to the extent possible
	 * without hash collisions). A partial match of the first or second component indicates related problems
	 * with the same root cause.
	 *
	 * @return A three-part error code
	 */
	@Override
	public String toString() {
		return methodCode() + DELIM + rootCauseCode() + DELIM + throwableCode();
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
	 * @param throwable The exception
	 * @return A three-part error code
	 */
	public static ErrorCode of(Throwable throwable) {
		var causalChain = Throwables.getCausalChain(throwable);
		if (causalChain.size() > 1) {
			var rootCause = causalChain.get(causalChain.size() - 1);
			var parentOfRootCause = causalChain.get(causalChain.size() - 2);
			var rootSpecificFrames = countTopmostFrames(rootCause.getStackTrace(), parentOfRootCause.getStackTrace());
			return new ErrorCode(throwable, rootCause, rootSpecificFrames);
		} else {
			return new ErrorCode(throwable, throwable, ALL_FRAMES);
		}
	}

	private String format(int value) {
		// Cut off highest 12 bits (only leave 20 least significant bits) and XOR rest with cutoff
		value = (value & 0xfffff) ^ (value >>> 20);
		return Strings.padStart(Integer.toString(value, 32).toUpperCase(Locale.ROOT), 4, '0');
	}

	private int traceCode(Throwable e, int frameCount) {
		int result = SEED;
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

	/**
	 * Counts the number of <em>additional</em> frames contained in <code>allFrames</code> but not in <code>bottomFrames</code>.
	 * <p>
	 * If <code>allFrames</code> does not end with <code>bottomFrames</code>, it is considered distinct and all its frames are counted.
	 *
	 * @param allFrames Some stack frames
	 * @param bottomFrames Other stack frames, potentially forming the bottom of the stack of <code>allFrames</code>
	 * @return The number of additional frames in <code>allFrames</code>. In most cases this should be equal to the difference in size.
	 */
	@VisibleForTesting
	static int countTopmostFrames(StackTraceElement[] allFrames, StackTraceElement[] bottomFrames) {
		if (allFrames.length < bottomFrames.length) {
			// if frames had been stacked on top of bottomFrames, allFrames would be larger
			return allFrames.length;
		} else {
			return allFrames.length - commonSuffixLength(allFrames, bottomFrames);
		}
	}

	@VisibleForTesting
	static <T> int commonSuffixLength(T[] set, T[] subset) {
		Preconditions.checkArgument(set.length >= subset.length);
		// iterate items backwards as long as they are identical
		var iterator = reverseStream(subset).iterator();
		return (int) reverseStream(set).takeWhile(item -> iterator.hasNext() && iterator.next().equals(item)).count();
	}

	private static <T> Stream<T> reverseStream(T[] array) {
		return IntStream.rangeClosed(1, array.length).mapToObj(i -> array[array.length - i]);
	}

}