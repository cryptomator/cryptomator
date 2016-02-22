package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.nio.OpenMode.WRITE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.cryptomator.filesystem.FileSystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class WritableNioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private FileSystem fileSystem;

	private Path path;

	private SharedFileChannel channel;

	private Runnable afterCloseCallback;

	private WritableNioFile inTest;

	@Before
	public void setup() {
		fileSystem = mock(FileSystem.class);
		channel = mock(SharedFileChannel.class);
		path = mock(Path.class);
		afterCloseCallback = mock(Runnable.class);
		inTest = new WritableNioFile(fileSystem, path, channel, afterCloseCallback);
	}

	public class ConstructorTests {

		@Test
		public void testConstructorSetsFileSystem() {
			assertThat(inTest.fileSystem(), is(fileSystem));
		}

		@Test
		public void testConstructorSetsPath() {
			assertThat(inTest.path(), is(path));
		}

		@Test
		public void testConstructorSetsChannel() {
			assertThat(inTest.channel(), is(channel));
		}

	}

	public class WriteTests {

		@Test
		public void testWriteOpensChannelIfClosedBeforeInvokingWriteFully() {
			ByteBuffer irrelevant = null;

			inTest.write(irrelevant);

			InOrder inOrder = inOrder(channel);
			inOrder.verify(channel).open(WRITE);
			inOrder.verify(channel).writeFully(0, irrelevant);
		}

		@Test
		public void testWriteDoesNotModifyBuffer() {
			ByteBuffer buffer = mock(ByteBuffer.class);

			inTest.write(buffer);

			verifyZeroInteractions(buffer);
		}

		@Test
		public void testWriteInvokesWriteFullyWithZeroPositionIfNotSet() {
			ByteBuffer buffer = mock(ByteBuffer.class);

			inTest.write(buffer);

			verify(channel).writeFully(0, buffer);
		}

		@Test
		public void testWriteInvokesWriteFullyWithSetPosition() {
			ByteBuffer buffer = mock(ByteBuffer.class);
			long position = 10;
			inTest.position(position);

			inTest.write(buffer);

			verify(channel).writeFully(position, buffer);
		}

		@Test
		public void testWriteInvokesWriteFullyWithEndOfPreviousWriteIfInvokedTwice() {
			ByteBuffer buffer = mock(ByteBuffer.class);
			int endOfPreviousWrite = 10;
			when(channel.writeFully(0, buffer)).thenReturn(endOfPreviousWrite);

			inTest.write(buffer);
			inTest.write(buffer);

			verify(channel).writeFully(endOfPreviousWrite, buffer);
		}

		@Test
		public void testWriteReturnsResultOfWriteFully() {
			ByteBuffer buffer = mock(ByteBuffer.class);
			int resultOfWriteFully = 14;
			when(channel.writeFully(0, buffer)).thenReturn(resultOfWriteFully);

			int result = inTest.write(buffer);

			assertThat(result, is(resultOfWriteFully));
		}

	}

	public class TruncateTests {

		@Test
		public void testTruncateInvokesChannelsOpenWithModeWriteIfInvokedForTheFirstTimeBeforeInvokingTruncate() {
			inTest.truncate();

			InOrder inOrder = inOrder(channel);
			inOrder.verify(channel).open(WRITE);
			inOrder.verify(channel).truncate(anyInt());
		}

		@Test
		public void testTruncateChannelsTruncateWithZeroAsParameter() {
			inTest.truncate();

			verify(channel).truncate(0);
		}

	}

	public class CloseTests {

		@Test
		public void testCloseClosesChannelIfOpenedAndInvokesAfterCloseCallback() {
			inTest.truncate();

			inTest.close();

			InOrder inOrder = inOrder(channel, afterCloseCallback);
			inOrder.verify(channel).close();
			inOrder.verify(afterCloseCallback).run();
		}

		@Test
		public void testCloseDoesNothingOnSecondInvocation() {
			inTest.truncate();

			inTest.close();
			inTest.close();

			InOrder inOrder = inOrder(channel, afterCloseCallback);
			inOrder.verify(channel).close();
			verify(afterCloseCallback).run();
		}

		@Test
		public void testCloseInvokesAfterCloseCallbackEvenIfCloseThrowsException() {
			inTest.truncate();
			String message = "exceptionMessage";
			doThrow(new RuntimeException(message)).when(channel).close();

			thrown.expectMessage(message);

			try {
				inTest.close();
			} finally {
				verify(afterCloseCallback).run();
			}
		}

	}

	public class IsOpenTests {

		@Test
		public void testIsOpenReturnsTrueForNewInstance() {
			assertThat(inTest.isOpen(), is(true));
		}

		@Test
		public void testIsOpenReturnsFalseForClosedInstance() {
			inTest.close();

			assertThat(inTest.isOpen(), is(false));
		}

	}

	public class OperationsFailIfClosedTests {

		@Test
		public void testWriteFailsIfClosed() {
			inTest.close();
			ByteBuffer irrelevant = null;

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.write(irrelevant);
		}

		@Test
		public void testPositionFailsIfClosed() {
			inTest.close();
			long irrelevant = 1023;

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.position(irrelevant);
		}

		@Test
		public void testTruncateFailsIfClosed() {
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.truncate();
		}

	}

	@Test
	public void testEnsureChannelIsOpenedInvokesChannelOpenWithModeWrite() {
		inTest.ensureChannelIsOpened();

		verify(channel).open(WRITE);
	}

	@Test
	public void testEnsureChannelIsOpenedInvokesChannelOpenWithModeWriteOnlyOnceIfInvokedTwice() {
		inTest.ensureChannelIsOpened();
		inTest.ensureChannelIsOpened();

		verify(channel).open(WRITE);
	}

	@Test
	public void testCloseChannelIfOpenInvokesChannelsCloseIfOpenedEarlier() {
		inTest.ensureChannelIsOpened();
		inTest.closeChannelIfOpened();

		verify(channel).close();
	}

	@Test
	public void testCloseChannelIfOpenDoesNotInvokeChannelsCloseIfNotOpenedEarlier() {
		inTest.closeChannelIfOpened();

		verifyZeroInteractions(channel);
	}

	@Test
	public void testToString() {
		assertThat(inTest.toString(), is(format("WritableNioFile(%s)", path)));
	}

}
