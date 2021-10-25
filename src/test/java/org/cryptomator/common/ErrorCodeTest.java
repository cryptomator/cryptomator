package org.cryptomator.common;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

public class ErrorCodeTest {

	private final StackTraceElement foo = new StackTraceElement("ErrorCodeTest", "foo", null, 0);
	private final StackTraceElement bar = new StackTraceElement("ErrorCodeTest", "bar", null, 0);
	private final StackTraceElement baz = new StackTraceElement("ErrorCodeTest", "baz", null, 0);
	private final Exception fooException = Mockito.mock(NullPointerException.class, "fooException");

	@Test
	@DisplayName("same exception leads to same error code")
	public void testDeterministicErrorCode() {
		Mockito.doReturn(new StackTraceElement[]{foo, bar, baz}).when(fooException).getStackTrace();
		var code1 = ErrorCode.of(fooException);
		var code2 = ErrorCode.of(fooException);

		Assertions.assertEquals(code1.toString(), code2.toString());
	}

	@Test
	@DisplayName("three error code segments change independently")
	public void testErrorCodeSegments() {
		Exception fooBarException = Mockito.mock(IndexOutOfBoundsException.class, "fooBarException");
		Mockito.doReturn(new StackTraceElement[]{foo, foo, foo}).when(fooBarException).getStackTrace();
		Mockito.doReturn(fooException).when(fooBarException).getCause();
		Mockito.doReturn(new StackTraceElement[]{bar, bar, bar, foo, foo, foo}).when(fooException).getStackTrace();

		var code = ErrorCode.of(fooBarException);

		Assertions.assertNotEquals(code.throwableCode(), code.rootCauseCode());
		Assertions.assertNotEquals(code.rootCauseCode(), code.methodCode());
	}

	@DisplayName("commonSuffixLength()")
	@ParameterizedTest
	@CsvSource({"1 2 3, 1 2 3, 3", "1 2 3, 0 2 3, 2", "1 2 3 4, 3 4, 2", "1 2 3 4, 5 6, 0", "1 2 3 4 5 6,, 0",})
	public void commonSuffixLength1(@ConvertWith(IntegerArrayConverter.class) Integer[] set, @ConvertWith(IntegerArrayConverter.class) Integer[] subset, int expected) {
		var result = ErrorCode.commonSuffixLength(set, subset);

		Assertions.assertEquals(expected, result);
	}

	@DisplayName("commonSuffixLength() with too short array")
	@ParameterizedTest
	@CsvSource({"1 2, 3 4 5 6", ",1 2 3 4 5 6",})
	public void commonSuffixLength2(@ConvertWith(IntegerArrayConverter.class) Integer[] set, @ConvertWith(IntegerArrayConverter.class) Integer[] subset) {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ErrorCode.commonSuffixLength(set, subset);
		});
	}

	@Test
	@DisplayName("countTopmostFrames() with partially overlapping suffix")
	public void testCountTopmostFrames1() {
		var allFrames = new StackTraceElement[]{foo, bar, baz, bar, foo};
		var bottomFrames = new StackTraceElement[]{baz, bar, foo};

		int result = ErrorCode.countTopmostFrames(allFrames, bottomFrames);

		Assertions.assertEquals(2, result);
	}

	@Test
	@DisplayName("countTopmostFrames() without overlapping suffix")
	public void testCountTopmostFrames2() {
		var allFrames = new StackTraceElement[]{foo, foo, foo};
		var bottomFrames = new StackTraceElement[]{bar, bar, bar};

		int result = ErrorCode.countTopmostFrames(allFrames, bottomFrames);

		Assertions.assertEquals(3, result);
	}

	@Test
	@DisplayName("countUniqueFrames() fully overlapping")
	public void testCountUniqueFrames3() {
		var allFrames = new StackTraceElement[]{foo, bar, baz};
		var bottomFrames = new StackTraceElement[]{foo, bar, baz};

		int result = ErrorCode.countTopmostFrames(allFrames, bottomFrames);

		Assertions.assertEquals(0, result);
	}

	@DisplayName("when different exception with same root cause")
	@Nested
	public class DifferentExceptionWithSameRootCause {

		private final Exception fooBarException = Mockito.mock(IllegalArgumentException.class, "fooBarException");
		private final Exception fooBazException = Mockito.mock(IndexOutOfBoundsException.class, "fooBazException");

		private ErrorCode code1;
		private ErrorCode code2;

		@BeforeEach
		public void setup() {
			Mockito.doReturn(new StackTraceElement[]{baz, bar, foo}).when(fooException).getStackTrace();
			Mockito.doReturn(new StackTraceElement[]{foo}).when(fooBarException).getStackTrace();
			Mockito.doReturn(new StackTraceElement[]{foo}).when(fooBazException).getStackTrace();
			Mockito.doReturn(fooException).when(fooBarException).getCause();
			Mockito.doReturn(fooException).when(fooBazException).getCause();
			this.code1 = ErrorCode.of(fooBarException);
			this.code2 = ErrorCode.of(fooBazException);
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

	@DisplayName("when same exception with different call stacks")
	@Nested
	public class SameExceptionDifferentCallStack {

		private final Exception barException = Mockito.mock(NullPointerException.class, "barException");

		private ErrorCode code1;
		private ErrorCode code2;

		@BeforeEach
		public void setup() {
			Mockito.doReturn(new StackTraceElement[]{foo, bar, baz}).when(fooException).getStackTrace();
			Mockito.doReturn(new StackTraceElement[]{foo, baz, bar}).when(barException).getStackTrace();
			this.code1 = ErrorCode.of(fooException);
			this.code2 = ErrorCode.of(barException);
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
		@DisplayName("rootCauseCodes are different")
		public void testSameRootCauseCodes() {
			Assertions.assertNotEquals(code1.rootCauseCode(), code2.rootCauseCode());
		}

		@Test
		@DisplayName("methodCode are equal")
		public void testSameMethodCodes() {
			Assertions.assertEquals(code1.methodCode(), code2.methodCode());
		}

	}

	public static class IntegerArrayConverter extends SimpleArgumentConverter {

		@Override
		protected Integer[] convert(Object source, Class<?> targetType) {
			if (source == null) {
				return new Integer[0];
			} else if (source instanceof String s && Integer[].class.isAssignableFrom(targetType)) {
				return Splitter.on(CharMatcher.inRange('0', '9').negate()).splitToStream(s).map(Integer::valueOf).toArray(Integer[]::new);
			} else {
				throw new IllegalArgumentException("Conversion from " + source.getClass() + " to " + targetType + " not supported.");
			}
		}

	}

}