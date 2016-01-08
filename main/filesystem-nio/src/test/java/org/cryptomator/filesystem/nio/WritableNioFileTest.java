package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.cryptomator.filesystem.nio.OpenMode.WRITE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;
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

	private NioAccess nioAccess;

	private WritableNioFile inTest;

	@Before
	public void setup() {
		fileSystem = mock(FileSystem.class);
		channel = mock(SharedFileChannel.class);
		path = mock(Path.class);
		afterCloseCallback = mock(Runnable.class);
		nioAccess = mock(NioAccess.class);
		inTest = new WritableNioFile(fileSystem, path, channel, afterCloseCallback, nioAccess);
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
			inOrder.verify(channel).openIfClosed(WRITE);
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

	public class MoveToTests {

		private WritableNioFile target;

		private Path pathOfTarget;

		@Before
		public void setUp() {
			target = mock(WritableNioFile.class);
			pathOfTarget = mock(Path.class);
			when(target.fileSystem()).thenReturn(fileSystem);
			when(target.path()).thenReturn(pathOfTarget);
		}

		@Test
		public void testMoveToFailsIfTargetIsNoWritableNioFile() {
			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage("Can only move to a WritableFile from the same FileSystem");

			inTest.moveTo(mock(WritableFile.class));
		}

		@Test
		public void testMoveToFailsIfTargetIsNotFromSameFileSystem() {
			WritableNioFile targetFromOtherFileSystem = mock(WritableNioFile.class);
			when(targetFromOtherFileSystem.fileSystem()).thenReturn(mock(FileSystem.class));

			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage("Can only move to a WritableFile from the same FileSystem");

			inTest.moveTo(mock(WritableFile.class));
		}

		@Test
		public void testMoveToFailsIfSourceIsDirectory() {
			when(nioAccess.isDirectory(path)).thenReturn(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("Source is a directory");
			thrown.expectMessage(path.toString());
			thrown.expectMessage(pathOfTarget.toString());

			inTest.moveTo(target);
		}

		@Test
		public void testMoveToFailsIfTargetIsDirectory() {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isDirectory(pathOfTarget)).thenReturn(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("Target is a directory");
			thrown.expectMessage(path.toString());
			thrown.expectMessage(pathOfTarget.toString());

			inTest.moveTo(target);
		}

		@Test
		public void testMoveToDoesNothingIfTargetIsSameInstance() {
			inTest.moveTo(inTest);

			verifyZeroInteractions(nioAccess);
		}

		@Test
		public void testMoveToAssertsTargetIsOpenClosesChannelsIfOpenMovesFileAndInvokesAfterCloseCallbacks() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isDirectory(pathOfTarget)).thenReturn(false);

			inTest.moveTo(target);

			InOrder inOrder = inOrder(target, nioAccess, channel, afterCloseCallback);
			inOrder.verify(target).assertOpen();
			inOrder.verify(channel).closeIfOpen();
			inOrder.verify(target).closeChannelIfOpened();
			inOrder.verify(nioAccess).move(path, pathOfTarget, REPLACE_EXISTING);
			inOrder.verify(target).invokeAfterCloseCallback();
			inOrder.verify(afterCloseCallback).run();
		}

		@Test
		public void testMoveToWrapsIOExceptionFromMoveInUncheckedIOException() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isDirectory(pathOfTarget)).thenReturn(false);
			IOException exceptionFromMove = new IOException();
			doThrow(exceptionFromMove).when(nioAccess).move(path, pathOfTarget, REPLACE_EXISTING);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromMove));

			inTest.moveTo(target);
		}

		@Test
		public void testMoveToInvokesAfterCloseCallbacksEvenIfMoveToThrowsException() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isDirectory(pathOfTarget)).thenReturn(false);
			IOException exceptionFromMove = new IOException();
			doThrow(exceptionFromMove).when(nioAccess).move(path, pathOfTarget, REPLACE_EXISTING);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromMove));

			inTest.moveTo(target);

			verify(target).invokeAfterCloseCallback();
			verify(afterCloseCallback).run();
		}

	}

	public class SetLastModifiedTests {

		@Test
		public void testSetLastModifiedOpensChannelIfClosedAndSetsLastModifiedTime() throws IOException {
			Instant instant = Instant.parse("2016-01-05T13:51:00Z");
			FileTime time = FileTime.from(instant);

			inTest.setLastModified(instant);

			InOrder inOrder = inOrder(channel, nioAccess);
			inOrder.verify(channel).openIfClosed(OpenMode.WRITE);
			inOrder.verify(nioAccess).setLastModifiedTime(path, time);
		}

		@Test
		public void testSetLastModifiedWrapsIOExceptionFromSetLastModifiedInUncheckedIOException() throws IOException {
			IOException exceptionFromSetLastModified = new IOException();
			Instant instant = Instant.parse("2016-01-05T13:51:00Z");
			FileTime time = FileTime.from(instant);
			doThrow(exceptionFromSetLastModified).when(nioAccess).setLastModifiedTime(path, time);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromSetLastModified));

			inTest.setLastModified(instant);
		}

	}

	public class SetCreationTimeTests {

		@Test
		public void testSetCreationTimeFailsIfNotOpen() {
			Instant irrelevant = null;
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.setCreationTime(irrelevant);
		}

		@Test
		public void testSetCreationTimeOpensChannelIfClosedAndInvokesNioAccessSetCreationTimeAfterwards() throws IOException {
			Instant instant = Instant.parse("2016-01-08T22:32:00Z");

			inTest.setCreationTime(instant);

			InOrder inOrder = inOrder(nioAccess, channel);
			inOrder.verify(channel).openIfClosed(OpenMode.WRITE);
			inOrder.verify(nioAccess).setCreationTime(path, FileTime.from(instant));
		}

		@Test
		public void testSetCreationTimeWrapsIOExceptionFromSetCreationTimeInUncheckedIOException() throws IOException {
			IOException exceptionFromSetCreationTime = new IOException();
			Instant irrelevant = Instant.now();
			doThrow(exceptionFromSetCreationTime).when(nioAccess).setCreationTime(same(path), any());

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromSetCreationTime));

			inTest.setCreationTime(irrelevant);
		}

	}

	public class DeleteTests {

		@Test
		public void testDeleteClosesChannelIfOpenAndDeletesFileAndInvokesAfterCloseCallback() throws IOException {
			inTest.delete();

			InOrder inOrder = inOrder(channel, nioAccess, afterCloseCallback);
			inOrder.verify(channel).closeIfOpen();
			inOrder.verify(nioAccess).delete(path);
			inOrder.verify(afterCloseCallback).run();
		}

		@Test
		public void testDeleteClosesWritableNioFile() {
			inTest.delete();

			assertThat(inTest.isOpen(), is(false));
		}

		@Test
		public void testDeleteClosesWritableNioFileEvenIfDeleteFails() throws IOException {
			IOException exceptionFromDelete = new IOException();
			doThrow(exceptionFromDelete).when(nioAccess).delete(path);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromDelete));

			try {
				inTest.delete();
			} finally {
				assertThat(inTest.isOpen(), is(false));
			}
		}

		@Test
		public void testDeleteInvokesAfterCloseCallbackEvenIfDeleteFails() throws IOException {
			IOException exceptionFromDelete = new IOException();
			doThrow(exceptionFromDelete).when(nioAccess).delete(path);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromDelete));

			try {
				inTest.delete();
			} finally {
				verify(afterCloseCallback).run();
			}
		}

	}

	public class TruncateTests {

		@Test
		public void testTruncateInvokesChannelsOpenWithModeWriteIfInvokedForTheFirstTimeBeforeInvokingTruncate() {
			inTest.truncate();

			InOrder inOrder = inOrder(channel);
			inOrder.verify(channel).openIfClosed(WRITE);
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
			inOrder.verify(channel).closeIfOpen();
			inOrder.verify(afterCloseCallback).run();
		}

		@Test
		public void testCloseDoesNothingOnSecondInvocation() {
			inTest.truncate();

			inTest.close();
			inTest.close();

			InOrder inOrder = inOrder(channel, afterCloseCallback);
			inOrder.verify(channel).closeIfOpen();
			verify(afterCloseCallback).run();
		}

		@Test
		public void testCloseInvokesAfterCloseCallbackEvenIfCloseThrowsException() {
			inTest.truncate();
			String message = "exceptionMessage";
			doThrow(new RuntimeException(message)).when(channel).closeIfOpen();

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

		@Test
		public void testMoveToFailsIfClosed() {
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.moveTo(null);
		}

		@Test
		public void testSetLastModifiedFailsIfClosed() {
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.setLastModified(null);
		}

		@Test
		public void testDeleteFailsIfClosed() {
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.delete();
		}

	}

	@Test
	public void testEnsureChannelIsOpenedInvokesChannelOpenIfClosedWithModeWrite() {
		inTest.ensureChannelIsOpened();

		verify(channel).openIfClosed(WRITE);
	}

	@Test
	public void testCloseChannelIfOpenInvokesChannelsCloseIfOpen() {
		inTest.closeChannelIfOpened();

		verify(channel).closeIfOpen();
	}

	@Test
	public void testToString() {
		assertThat(inTest.toString(), is(format("WritableNioFile(%s)", path)));
	}

}
