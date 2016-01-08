package org.cryptomator.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.Test;

public class CachingSupplierTest {

	@Test
	public void testInvokingGetInvokesDelegate() {
		@SuppressWarnings("unchecked")
		Supplier<Object> delegate = mock(Supplier.class);
		Object expectedResult = new Object();
		when(delegate.get()).thenReturn(expectedResult);
		Supplier<Object> inTest = CachingSupplier.from(delegate);

		Object result = inTest.get();

		assertThat(result, is(expectedResult));
	}

	@Test
	public void testInvokingGetTwiceDoesNotInvokeDelegateTwice() {
		@SuppressWarnings("unchecked")
		Supplier<Object> delegate = mock(Supplier.class);
		Object expectedResult = new Object();
		when(delegate.get()).thenReturn(expectedResult);
		Supplier<Object> inTest = CachingSupplier.from(delegate);

		inTest.get();
		Object result = inTest.get();

		assertThat(result, is(expectedResult));
		verify(delegate).get();
	}

}
