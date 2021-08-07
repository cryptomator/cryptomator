package org.cryptomator.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
public class ErrorCodeGenerator {


	private final static int A_PRIME = Integer.MAX_VALUE;

	private final static int NO_CAUSES = 0;
	private final static int LATEST_FRAME = 1;
	private final static int ALL_FRAMES = -1;
	private final static int ALL_CAUSES = -1;

	@Inject
	ErrorCodeGenerator() {
		//NO-OP
	}

	public String of(Throwable e) {
		Preconditions.checkNotNull(e);
		return format(traceCode(rootCause(e), NO_CAUSES, LATEST_FRAME)) + ':' + format(traceCode(e, ALL_CAUSES, ALL_FRAMES));
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

	private int traceCode(Throwable e, int causeDepth, int frameCount) {
		int result = 0x6c528c4a;
		if (causeDepth != 0 && e.getCause() != null) {
			result = traceCode(e.getCause(), (causeDepth == ALL_CAUSES ? ALL_CAUSES : causeDepth - 1), frameCount);
		}
		result = result * A_PRIME + e.getClass().getName().hashCode();
		var stack = e.getStackTrace();
		for (int i = 0; i < stack.length && (frameCount == ALL_FRAMES || i < frameCount); i++) {
			result = result * A_PRIME + stack[i].getClassName().hashCode();
			result = result * A_PRIME + stack[i].getMethodName().hashCode();
		}
		return result;
	}
}