package org.cryptomator.common;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.cryptomator.common.WeakValuedCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

public class WeakValuedCacheTest {

	private final String A_KEY = "aKey";
	private final String ANOTHER_KEY = "anotherKey";

	private WeakValuedCache<String, Value> inTest;

	private Function<String, Value> loader;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		loader = Mockito.mock(Function.class);
		inTest = WeakValuedCache.usingLoader(loader);
	}

	@Test
	public void testResultOfGetIsResultOfLoaderForTheSameKey() {
		Value theValue = new Value();
		Value theOtherValue = new Value();
		when(loader.apply(A_KEY)).thenReturn(theValue);
		when(loader.apply(ANOTHER_KEY)).thenReturn(theOtherValue);

		Value result = inTest.get(A_KEY);
		Value anotherResult = inTest.get(ANOTHER_KEY);

		assertThat(result, is(sameInstance(theValue)));
		assertThat(anotherResult, is(sameInstance(theOtherValue)));
	}

	@Test
	public void testCachedResultIsResultOfLoaderForTheSameKey() {
		Value theValue = new Value();
		Value theOtherValue = new Value();
		when(loader.apply(A_KEY)).thenReturn(theValue);
		when(loader.apply(ANOTHER_KEY)).thenReturn(theOtherValue);

		inTest.get(A_KEY);
		inTest.get(ANOTHER_KEY);
		Value result = inTest.get(A_KEY);
		Value anotherResult = inTest.get(ANOTHER_KEY);

		assertThat(result, is(sameInstance(theValue)));
		assertThat(anotherResult, is(sameInstance(theOtherValue)));
	}

	@Test
	public void testTwiceInvocationOfGetDoesNotInvokeLoaderTwice() {
		Value theValue = new Value();
		when(loader.apply(A_KEY)).thenReturn(theValue);

		inTest.get(A_KEY);
		inTest.get(A_KEY);

		verify(loader).apply(A_KEY);
	}

	@Test
	public void testSecondInvocationOfGetReturnsTheSameResult() {
		Value theValue = new Value();
		when(loader.apply(A_KEY)).thenReturn(theValue);

		inTest.get(A_KEY);
		Value result = inTest.get(A_KEY);

		assertThat(result, is(sameInstance(theValue)));
	}

	@Test
	public void testCacheDoesNotPreventGarbageCollectionOfValues() {
		when(loader.apply(A_KEY)).thenAnswer(this::createValueUsingMoreThanHalfTheJvmMemory);

		inTest.get(A_KEY);

		// force garbage collection of previously created value by creating an
		// object so large it can not coexist with the value
		createObjectUsingMoreThanHalfTheJvmMemory();
	}

	@Test(expected = RuntimeExceptionThrownInLoader.class)
	public void testCacheRethrowsRuntimeExceptionsFromLoader() {
		when(loader.apply(A_KEY)).thenThrow(new RuntimeExceptionThrownInLoader());

		inTest.get(A_KEY);
	}

	@Test(expected = ErrorThrownInLoader.class)
	public void testCacheRethrowsErrorsFromLoader() {
		when(loader.apply(A_KEY)).thenThrow(new ErrorThrownInLoader());

		inTest.get(A_KEY);
	}

	private Value createValueUsingMoreThanHalfTheJvmMemory(InvocationOnMock invocation) {
		Object data = createObjectUsingMoreThanHalfTheJvmMemory();
		Value value = new Value();
		value.setPayload(data);
		return value;
	}

	private Object createObjectUsingMoreThanHalfTheJvmMemory() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		long moreThanHalfTheJvmMemory = maxMemory / 2 + 1;
		return createObjectUsingAtLeast(moreThanHalfTheJvmMemory);
	}

	private Object createObjectUsingAtLeast(long minMemory) {
		if (minMemory <= Integer.MAX_VALUE) {
			return new byte[(int) minMemory];
		} else if ((minMemory / Integer.MAX_VALUE) <= Integer.MAX_VALUE) {
			int numberOfArraysWithMaxIntSize = (int) (minMemory / Integer.MAX_VALUE);
			int numberOfRemainingBytes = (int) (minMemory - Integer.MAX_VALUE * numberOfArraysWithMaxIntSize);
			return new byte[][][] { //
					new byte[numberOfArraysWithMaxIntSize][Integer.MAX_VALUE], //
					new byte[1][numberOfRemainingBytes] //
			};
		} else {
			throw new IllegalArgumentException(format("Can not create object with more than 3.999999996 Exabyte"));
		}
	}

	private static class RuntimeExceptionThrownInLoader extends RuntimeException {
	}

	private static class ErrorThrownInLoader extends Error {
	}

	private static class Value {

		@SuppressWarnings("unused")
		private Object payload;

		public void setPayload(Object payload) {
			this.payload = payload;
		}
	}

}
