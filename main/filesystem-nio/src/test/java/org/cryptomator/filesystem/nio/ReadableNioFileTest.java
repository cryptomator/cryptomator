package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.nio.OpenMode.READ;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class ReadableNioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Path path;

	private SharedFileChannel channel;

	private Runnable afterCloseCallback;

	private ReadableNioFile inTest;

	@Before
	public void setup() {
		path = mock(Path.class);
		channel = mock(SharedFileChannel.class);
		afterCloseCallback = mock(Runnable.class);

		inTest = new ReadableNioFile(path, channel, afterCloseCallback);
	}

	@Test
	public void testConstructorInvokesOpenWithReadModeOnChannelOfNioFile() {
		verify(channel).open(READ);
	}

	@Test
	public void testReadFailsIfClosed() {
		ByteBuffer irrelevant = null;
		inTest.close();

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.read(irrelevant);
	}

	@Test
	public void testPositionFailsIfClosed() {
		int irrelevant = 1;
		inTest.close();

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.position(irrelevant);
	}

	@Test
	public void testPositionFailsIfNegative() {

		thrown.expect(IllegalArgumentException.class);

		inTest.position(-1);
	}

	@Test
	public void testSizeReturnsSizeOfChannel() {
		long expectedSize = 85472;
		when(channel.size()).thenReturn(expectedSize);

		long actualSize = inTest.size();

		assertThat(actualSize, is(expectedSize));
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithZeroPositionIfNotSet() {
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.read(buffer);

		verify(channel).readFully(0, buffer);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithPositionAtEndOfPreviousReadIfInvokedTwice() {
		ByteBuffer buffer = mock(ByteBuffer.class);
		int endOfPreviousRead = 10;
		when(channel.readFully(0, buffer)).thenReturn(endOfPreviousRead);

		inTest.read(buffer);
		inTest.read(buffer);

		verify(channel).readFully(0, buffer);
		verify(channel).readFully(10, buffer);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithPositionUnchangedIfPreviousReadReturnedEof() {
		ByteBuffer buffer = mock(ByteBuffer.class);
		when(channel.readFully(0, buffer)).thenReturn(SharedFileChannel.EOF);

		inTest.read(buffer);
		inTest.read(buffer);

		verify(channel, times(2)).readFully(0, buffer);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithSetPosition() {
		ByteBuffer buffer = mock(ByteBuffer.class);
		int position = 10;
		inTest.position(position);

		inTest.read(buffer);

		verify(channel).readFully(position, buffer);
	}

	@Test
	public void testReadReturnsValueOfChannelReadFully() {
		ByteBuffer buffer = mock(ByteBuffer.class);
		int expectedResult = 37028;
		when(channel.readFully(0, buffer)).thenReturn(expectedResult);

		int result = inTest.read(buffer);

		assertThat(result, is(expectedResult));
	}

	@Test
	public void testReadDoesNotModifyBuffer() {
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.read(buffer);

		verifyZeroInteractions(buffer);
	}

	@Test
	public void testIsOpenReturnsTrueForNewReadableNioFile() {
		assertThat(inTest.isOpen(), is(true));
	}

	@Test
	public void testIsOpenReturnsFalseForClosed() {
		inTest.close();

		assertThat(inTest.isOpen(), is(false));
	}

	@Test
	public void testCloseClosesChannelAndUnlocksReadLock() {
		inTest.close();

		InOrder inOrder = Mockito.inOrder(channel, afterCloseCallback);
		inOrder.verify(channel).close();
		inOrder.verify(afterCloseCallback).run();
	}

	@Test
	public void testCloseClosesChannelAndUnlocksReadLockOnlyOnceIfInvokedTwice() {
		inTest.close();
		inTest.close();

		InOrder inOrder = Mockito.inOrder(channel, afterCloseCallback);
		inOrder.verify(channel).close();
		inOrder.verify(afterCloseCallback).run();
	}

	@Test
	public void testCloseUnlocksReadLockEvenIfCloseFails() {
		String message = "exceptionMessage";
		doThrow(new RuntimeException(message)).when(channel).close();

		thrown.expectMessage(message);

		try {
			inTest.close();
		} finally {
			verify(afterCloseCallback).run();
		}
	}

	@Test
	public void testToString() {
		assertThat(inTest.toString(), is(format("ReadableNioFile(%s)", path)));
	}

}
