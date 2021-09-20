package org.cryptomator.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ErrorCodeTest {

	private static ErrorCode codeCaughtFrom(RunnableThrowingException<RuntimeException> runnable) {
		try {
			runnable.run();
			throw new IllegalStateException("should not reach this point");
		} catch (RuntimeException e) {
			return new ErrorCode(e);
		}
	}

	@Test
	@DisplayName("same exception leads to same error code")
	public void testDifferentErrorCodes() {
		var code1 = codeCaughtFrom(this::foo);
		var code2 = codeCaughtFrom(this::foo);

		Assertions.assertEquals(code1.toString(), code2.toString());
	}

	private void foo() throws RuntimeException {
		throw new NullPointerException();
	}

	@DisplayName("when different cause but same root cause")
	@Nested
	public class SameRootCauseDifferentCause {

		private final ErrorCode code1 = codeCaughtFrom(this::foo);
		private final ErrorCode code2 = codeCaughtFrom(this::bar);

		private void foo() throws NullPointerException {
			throw new NullPointerException();
		}

		private void bar() throws IllegalArgumentException {
			try {
				foo();
			} catch (NullPointerException e) {
				throw new IllegalArgumentException(e);
			}
		}

		@Test
		@DisplayName("error codes are different")
		public void testDifferentCodes() {
			Assertions.assertNotEquals(code1.toString(), code2.toString());
		}

		@Test
		@DisplayName("causes are different")
		public void testDifferentCauses() {
			Assertions.assertNotEquals(code1.origCauseCode(), code2.origCauseCode());
		}

		@Test
		@DisplayName("root causes are equal")
		public void testSameRootCause() {
			Assertions.assertEquals(code1.rootCauseCode(), code2.rootCauseCode());
		}

		@Test
		@DisplayName("method throwing exception is the same")
		public void testSameMethods() {
			Assertions.assertEquals(code1.methodCode(), code2.methodCode());
		}

	}

	@DisplayName("when same cause but different call stack")
	@Nested
	public class SameCauseDifferentCallStack {

		private final ErrorCode code1 = codeCaughtFrom(this::foo);
		private final ErrorCode code2 = codeCaughtFrom(this::bar);

		private void foo() throws NullPointerException {
			throw new NullPointerException();
		}

		private void bar() throws NullPointerException {
			foo();
		}

		@Test
		@DisplayName("error codes are different")
		public void testDifferentCodes() {
			Assertions.assertNotEquals(code1.toString(), code2.toString());
		}

		@Test
		@DisplayName("causes are equal")
		public void testDifferentCauses() {
			Assertions.assertEquals(code1.origCauseCode(), code2.origCauseCode());
		}

		@Test
		@DisplayName("root causes are equal")
		public void testSameRootCause() {
			Assertions.assertEquals(code1.rootCauseCode(), code2.rootCauseCode());
		}

		@Test
		@DisplayName("method throwing exception is the same")
		public void testSameMethods() {
			Assertions.assertEquals(code1.methodCode(), code2.methodCode());
		}

	}

}