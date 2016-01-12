package org.cryptomator.common;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

@SuppressWarnings("unchecked")
public class AutoClosingStreamTest {

	private Stream<Object> delegate;
	private Stream<Object> inTest;

	@Before
	public void setUp() {
		delegate = mock(Stream.class);
		inTest = AutoClosingStream.from(delegate);
	}

	@Test
	public void testSequentialReturnsNewAutoClosingStream() {
		Stream<Object> newDelegate = mock(Stream.class);
		when(delegate.sequential()).thenReturn(newDelegate);

		Stream<Object> result = inTest.sequential();

		assertThat(result, is(instanceOf(AutoClosingStream.class)));
		verifyDelegate(result, newDelegate);
	}

	@Test
	public void testForEachDelegatesToAndClosesDelegate() {
		Consumer<Object> consumer = mock(Consumer.class);

		inTest.forEach(consumer);

		InOrder inOrder = inOrder(delegate);
		inOrder.verify(delegate).forEach(consumer);
		inOrder.verify(delegate).close();
	}

	private void verifyDelegate(Stream<Object> result, Stream<Object> newDelegate) {
		result.close();
		verify(newDelegate).close();
	}

	// TODO Markus Kreusch test additional methods

}
