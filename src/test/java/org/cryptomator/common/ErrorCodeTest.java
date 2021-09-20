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
			return ErrorCode.create(e);
		}
	}

	@Test
	@DisplayName("same exception leads to same error code")
	public void testDifferentErrorCodes() {
		var code1 = codeCaughtFrom(this::throwNpe);
		var code2 = codeCaughtFrom(this::throwNpe);

		Assertions.assertEquals(code1.toString(), code2.toString());
	}

	private void throwNpe() {
		throwException(new NullPointerException());
	}

	private void throwException(RuntimeException e) throws RuntimeException {
		throw e;
	}

	@DisplayName("when different cause but same root cause")
	@Nested
	public class SameRootCauseDifferentCause {

		private final ErrorCode code1 = codeCaughtFrom(this::foo);
		private final ErrorCode code2 = codeCaughtFrom(this::bar);

		private void foo() throws IllegalArgumentException {
			try {
				throwNpe();
			} catch (NullPointerException e) {
				throw new IllegalArgumentException(e);
			}
		}

		private void bar() throws IllegalStateException {
			try {
				throwNpe();
			} catch (NullPointerException e) {
				throw new IllegalStateException(e);
			}
		}

		@Test
		@DisplayName("error codes are different")
		public void testDifferentCodes() {
			Assertions.assertNotEquals(code1.toString(), code2.toString());
		}

		@Test
		@DisplayName("throwableCodes are different")
		public void testDifferentThrowableCodes() {
			Assertions.assertNotEquals(code1.throwableCode(), code2.throwableCode());
		}

		@Test
		@DisplayName("rootCauseCodes are equal")
		public void testSameRootCauseCodes() {
			Assertions.assertEquals(code1.rootCauseCode(), code2.rootCauseCode());
		}

		@Test
		@DisplayName("methodCode are equal")
		public void testSameMethodCodes() {
			Assertions.assertEquals(code1.methodCode(), code2.methodCode());
		}

	}

	@DisplayName("when same cause but different call stack")
	@Nested
	public class SameCauseDifferentCallStack {

		private final ErrorCode code1 = codeCaughtFrom(this::foo);
		private final ErrorCode code2 = codeCaughtFrom(this::bar);

		private void foo() throws NullPointerException {
			try {
				throwNpe();
			} catch (NullPointerException e) {
				throw new IllegalArgumentException(e);
			}
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
		@DisplayName("throwableCodes are different")
		public void testDifferentThrowableCodes() {
			Assertions.assertNotEquals(code1.throwableCode(), code2.throwableCode());
		}

		@Test
		@DisplayName("rootCauseCodes are equal")
		public void testSameRootCauseCodes() {
			Assertions.assertEquals(code1.rootCauseCode(), code2.rootCauseCode());
		}

		@Test
		@DisplayName("methodCode are equal")
		public void testSameMethodCodes() {
			Assertions.assertEquals(code1.methodCode(), code2.methodCode());
		}

	}

}