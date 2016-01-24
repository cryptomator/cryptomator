package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.cryptomator.common.test.matcher.ExceptionMatcher.ofType;
import static org.cryptomator.filesystem.nio.SharedFileChannel.EOF;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.cryptomator.common.Holder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class SharedFileChannelTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Path path;
	private NioAccess nioAccess;

	private SharedFileChannel inTest;

	@Before
	public void setUp() {
		path = mock(Path.class);
		nioAccess = mock(NioAccess.class);
		inTest = new SharedFileChannel(path, nioAccess);
	}

	public class Open {

		@Test
		public void testOpenFailsIfPathIsADirectory() {
			when(nioAccess.isDirectory(path)).thenReturn(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s is a directory", path));

			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testOpenFailsIfFileDoesNotExistAndOpenModeIsRead() {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s does not exist", path));

			inTest.open(OpenMode.READ);
		}

		@Test
		public void testOpenOpensAChannelIfOpenModeIsReadAndFileExists() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			inTest.open(OpenMode.READ);

			verify(nioAccess).open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		}

		@Test
		public void testOpenOpensAChannelIfOpenModeIsWriteAndFileExists() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			inTest.open(OpenMode.WRITE);

			verify(nioAccess).open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		}

		@Test
		public void testOpenOpensAChannelIfOpenModeIsWriteAndFileDoesNotExist() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);

			inTest.open(OpenMode.WRITE);

			verify(nioAccess).open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		}

		@Test
		public void testOpenWrapsExceptionsFromOpeningChannelInUncheckedIOExceptions() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			IOException exceptionFromOpeningChannel = new IOException();
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenThrow(exceptionFromOpeningChannel);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromOpeningChannel));

			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testOpenDoesNotOpenChannelTwiceIfInvokedTwiceByDifferentThreads() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(mock(AsynchronousFileChannel.class));

			inThreadRethrowingException(() -> inTest.open(OpenMode.WRITE));
			inThreadRethrowingException(() -> inTest.open(OpenMode.WRITE));

			verify(nioAccess).open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		}

	}

	public class Close {

		@Test
		public void testCloseIfNotOpenFails() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Close without corresponding open");

			inTest.close();
		}

		@Test
		public void testCloseIfClosedFails() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(mock(AsynchronousFileChannel.class));
			inTest.open(OpenMode.WRITE);
			inTest.close();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Close without corresponding open");

			inTest.close();
		}

		@Test
		public void testCloseForcesAndClosesChannel() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);

			inTest.close();

			InOrder inOrder = inOrder(channel, nioAccess);
			inOrder.verify(channel).force(true);
			inOrder.verify(nioAccess).close(channel);
		}

		@Test
		public void testCloseWrapsIOExceptionFromForceInUncheckedIOExceptionAndStillClosesChannel() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
			IOException exceptionFromForce = new IOException();
			doThrow(exceptionFromForce).when(channel).force(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromForce));

			try {
				inTest.close();
			} finally {
				verify(nioAccess).close(channel);
			}
		}

		@Test
		public void testCloseWrapsIOExceptionFromCloseInUncheckedIOException() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
			IOException exceptionFromClose = new IOException();
			doThrow(exceptionFromClose).when(nioAccess).close(channel);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromClose));

			inTest.close();
		}

		@Test
		public void testCloseDoesNotCloseChannelIfOpenedTwice() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
			inThreadRethrowingException(() -> inTest.open(OpenMode.WRITE));

			inTest.close();

			verify(nioAccess, never()).close(channel);
		}

		@Test
		public void testLastCloseDoesCloseChannelIfOpenedTwice() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(false);
			AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
			inThreadRethrowingException(() -> {
				inTest.open(OpenMode.WRITE);
				inTest.close();
			});

			inTest.close();

			verify(nioAccess).close(channel);
		}

	}

	public class ReadFully {

		@Rule
		public Timeout timeoutRule = Timeout.seconds(1);

		private AsynchronousFileChannel channel;

		@Before
		public void setUp() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.READ);
		}

		@Test
		public void testReadFullyWrapsExceptionFromReadInUncheckedIOException() throws InterruptedException, ExecutionException {
			ByteBuffer buffer = ByteBuffer.allocate(0);
			ExecutionException exceptionFromRead = new ExecutionException(new IOException());
			@SuppressWarnings("unchecked")
			Future<Integer> result = mock(Future.class);
			when(channel.read(buffer, 0)).thenReturn(result);
			when(result.get()).thenThrow(exceptionFromRead);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(ofType(IOException.class).withCauseThat(is(exceptionFromRead))));

			inTest.readFully(0, buffer);
		}

		@Test
		public void testReadFullyDelegatesToChannelRead() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(50);
			when(channel.read(buffer, 0)).thenAnswer(new Answer<Future<Integer>>() {
				@Override
				public Future<Integer> answer(InvocationOnMock invocation) throws Throwable {
					buffer.position(50);
					return constantFuture(50);
				}
			});

			int result = inTest.readFully(0, buffer);

			assertThat(result, is(50));
			verify(channel).read(buffer, 0);
			verifyNoMoreInteractions(channel);
		}

		@Test
		public void testReadFullyReturnsEofWhenFirstReadReturnsIt() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(50);
			when(channel.read(buffer, 0)).thenReturn(constantFuture(EOF));

			int result = inTest.readFully(0, buffer);

			assertThat(result, is(EOF));
			verify(channel).read(buffer, 0);
			verifyNoMoreInteractions(channel);
		}

		@Test
		public void testReadStopsReadingIfEofIsReached() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(50);
			when(channel.read(buffer, 0)).thenAnswer(simulateRead(20, buffer));
			when(channel.read(buffer, 20)).thenReturn(constantFuture(EOF));

			int result = inTest.readFully(0, buffer);

			assertThat(result, is(20));
			verify(channel).read(buffer, 0);
			verify(channel).read(buffer, 20);
			verifyNoMoreInteractions(channel);
		}

		@Test
		public void testReadFullyInvokesReadUntilBufferIsFull() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(50);
			when(channel.read(buffer, 0)).then(simulateRead(20, buffer));
			when(channel.read(buffer, 20)).then(simulateRead(20, buffer));
			when(channel.read(buffer, 40)).then(simulateRead(10, buffer));

			int result = inTest.readFully(0, buffer);

			assertThat(result, is(50));
			verify(channel).read(buffer, 0);
			verify(channel).read(buffer, 20);
			verify(channel).read(buffer, 40);
			verifyNoMoreInteractions(channel);
		}

		private Answer<Future<Integer>> simulateRead(int amount, ByteBuffer target) {
			return new Answer<Future<Integer>>() {
				@Override
				public Future<Integer> answer(InvocationOnMock invocation) throws Throwable {
					target.position(target.position() + amount);
					return constantFuture(amount);
				}
			};
		}

	}

	public class Truncate {

		private AsynchronousFileChannel channel;

		@Before
		public void setUp() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testTruncateDelegatesToChannelTruncate() throws IOException {
			int truncateTo = 32;

			inTest.truncate(truncateTo);

			verify(channel).truncate(truncateTo);
		}

		@Test
		public void testTruncateWrapsIOExceptionInUncheckedIOException() throws IOException {
			int truncateTo = 32;
			IOException exceptionFromTruncate = new IOException();
			when(channel.truncate(truncateTo)).thenThrow(exceptionFromTruncate);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromTruncate));

			inTest.truncate(truncateTo);
		}

	}

	public class Size {

		private AsynchronousFileChannel channel;

		@Before
		public void setUp() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testSizeDelegatesToChannelSize() throws IOException {
			long expectedSize = 832;
			when(channel.size()).thenReturn(expectedSize);

			long result = inTest.size();

			assertThat(result, is(expectedSize));
		}

		@Test
		public void testSizeWrapsIOExceptionInUncheckedIOException() throws IOException {
			IOException exceptionFromSize = new IOException();
			when(channel.size()).thenThrow(exceptionFromSize);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromSize));

			inTest.size();
		}

	}

	// TODO fix / implement tests
	// public class TransferTo {
	//
	// private AsynchronousFileChannel channel;
	//
	// private Path targetPath;
	// private SharedFileChannel targetInTest;
	// private AsynchronousFileChannel targetChannel;
	//
	// @Before
	// public void setUp() throws IOException {
	// targetPath = mock(Path.class);
	// targetInTest = new SharedFileChannel(targetPath, nioAccess);
	//
	// when(nioAccess.isDirectory(path)).thenReturn(false);
	// when(nioAccess.isRegularFile(path)).thenReturn(true);
	// channel = mock(AsynchronousFileChannel.class);
	// when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
	// inTest.open(OpenMode.WRITE);
	//
	// when(nioAccess.isDirectory(targetPath)).thenReturn(false);
	// when(nioAccess.isRegularFile(targetPath)).thenReturn(true);
	// targetChannel = mock(AsynchronousFileChannel.class);
	// when(nioAccess.open(targetPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(targetChannel);
	// targetInTest.open(OpenMode.WRITE);
	// }
	//
	// @Test
	// public void testTransferToThrowsIllegalArugmentExceptionIfCountIsNegative() {
	// thrown.expect(IllegalArgumentException.class);
	// thrown.expectMessage("Count must not be negative");
	//
	// inTest.transferTo(0, -1, targetInTest, 0);
	// }
	//
	// @Test
	// public void testTransferToSetsPositionOfTargetChannelAndThenDelegatesToChannelsTransferTo() throws IOException {
	// long targetPosition = 43L;
	// long startPosition = 22L;
	// long count = 39L;
	// when(channel.transferTo(startPosition, count, targetChannel)).thenReturn(count);
	// when(channel.size()).thenReturn(startPosition + count);
	//
	// long result = inTest.transferTo(startPosition, count, targetInTest, targetPosition);
	//
	// assertThat(result, is(count));
	// InOrder inOrder = inOrder(channel, targetChannel);
	// inOrder.verify(targetChannel).position(targetPosition);
	// inOrder.verify(channel).transferTo(startPosition, count, targetChannel);
	// }
	//
	// @Test
	// public void testTransferToInvokesTransferUntilAllBytesHaveBeenTransferred() throws IOException {
	// long targetPosition = 43L;
	// long startPosition = 22L;
	// long count = 39L;
	// long firstTransferCount = 10L;
	// long secondTransferCount = 7L;
	// long thridTransferCount = count - firstTransferCount - secondTransferCount;
	// when(channel.transferTo(startPosition, count, targetChannel)).thenReturn(firstTransferCount);
	// when(channel.transferTo(startPosition + firstTransferCount, count - firstTransferCount, targetChannel)).thenReturn(secondTransferCount);
	// when(channel.transferTo(startPosition + firstTransferCount + secondTransferCount, thridTransferCount, targetChannel)).thenReturn(thridTransferCount);
	// when(channel.size()).thenReturn(startPosition + count);
	//
	// long result = inTest.transferTo(startPosition, count, targetInTest, targetPosition);
	//
	// assertThat(result, is(count));
	// InOrder inOrder = inOrder(channel, targetChannel);
	// inOrder.verify(targetChannel).position(targetPosition);
	// inOrder.verify(channel).transferTo(startPosition, count, targetChannel);
	// inOrder.verify(channel).transferTo(startPosition + firstTransferCount, count - firstTransferCount, targetChannel);
	// inOrder.verify(channel).transferTo(startPosition + firstTransferCount + secondTransferCount, thridTransferCount, targetChannel);
	// }
	//
	// @Test
	// public void testTransferToStopsTransferAtEndOfSourceFile() throws IOException {
	// long targetPosition = 43L;
	// long startPosition = 22L;
	// long count = 39L;
	// long countAvailable = 30L;
	// when(channel.transferTo(startPosition, countAvailable, targetChannel)).thenReturn(countAvailable);
	// when(channel.size()).thenReturn(startPosition + countAvailable);
	//
	// long result = inTest.transferTo(startPosition, count, targetInTest, targetPosition);
	//
	// assertThat(result, is(countAvailable));
	// InOrder inOrder = inOrder(channel, targetChannel);
	// inOrder.verify(targetChannel).position(targetPosition);
	// inOrder.verify(channel).transferTo(startPosition, countAvailable, targetChannel);
	// }
	//
	// @Test
	// public void testTransferToWrapsIOExceptionFromPositionInUncheckedIOException() throws IOException {
	// when(channel.size()).thenReturn(Long.MAX_VALUE);
	// IOException exceptionFromPosition = new IOException();
	// when(targetChannel.position(anyLong())).thenThrow(exceptionFromPosition);
	//
	// thrown.expect(UncheckedIOException.class);
	// thrown.expectCause(is(exceptionFromPosition));
	//
	// inTest.transferTo(0L, 10L, targetInTest, 0L);
	// }
	//
	// @Test
	// public void testTransferToWrapsIOExceptionFromTransferToInUncheckedIOException() throws IOException {
	// when(channel.size()).thenReturn(Long.MAX_VALUE);
	// IOException exceptionFromTransferTo = new IOException();
	// when(channel.transferTo(anyLong(), anyLong(), any())).thenThrow(exceptionFromTransferTo);
	//
	// thrown.expect(UncheckedIOException.class);
	// thrown.expectCause(is(exceptionFromTransferTo));
	//
	// inTest.transferTo(0L, 10L, targetInTest, 0L);
	// }
	//
	// }

	public class WriteFully {

		@Rule
		public Timeout timeoutRule = Timeout.seconds(1);

		private AsynchronousFileChannel channel;

		@Before
		public void setUp() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			channel = mock(AsynchronousFileChannel.class);
			when(nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)).thenReturn(channel);
			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testWriteFullyWrapsIOExceptionFromWriteIntoUncheckedIOException() throws InterruptedException, ExecutionException {
			int count = 1;
			int position = 0;
			ByteBuffer buffer = ByteBuffer.allocate(count);
			ExecutionException exceptionFromWrite = new ExecutionException(new IOException());
			@SuppressWarnings("unchecked")
			Future<Integer> result = mock(Future.class);
			when(channel.write(buffer, position)).thenReturn(result);
			when(result.get()).thenThrow(exceptionFromWrite);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(ofType(IOException.class).withCauseThat(is(exceptionFromWrite))));

			inTest.writeFully(position, buffer);
		}

		@Test
		public void testWriteFullyDelegatesToChannelsWrite() throws IOException {
			int count = 50;
			int position = 31;
			ByteBuffer buffer = ByteBuffer.allocate(count);
			when(channel.write(buffer, position)).then(simulateWrite(count, buffer));

			int result = inTest.writeFully(position, buffer);

			assertThat(result, is(count));
			verify(channel).write(buffer, position);
		}

		@Test
		public void testWriteFullyDelegatesToWriteUntilAllBytesFromBufferHaveBeenWritten() throws IOException {
			int count = 50;
			int countOfFirstWrite = 10;
			int countOfSecondWrite = 15;
			int countOfThridWrite = count - countOfFirstWrite - countOfSecondWrite;
			int position = 31;
			ByteBuffer buffer = ByteBuffer.allocate(count);
			when(channel.write(buffer, position)).then(simulateWrite(countOfFirstWrite, buffer));
			when(channel.write(buffer, position + countOfFirstWrite)).then(simulateWrite(countOfSecondWrite, buffer));
			when(channel.write(buffer, position + countOfFirstWrite + countOfSecondWrite)).then(simulateWrite(countOfThridWrite, buffer));

			int result = inTest.writeFully(position, buffer);

			assertThat(result, is(count));
			verify(channel).write(buffer, position);
			verify(channel).write(buffer, position + countOfFirstWrite);
			verify(channel).write(buffer, position + countOfFirstWrite + countOfSecondWrite);
		}

		@Test
		public void testWriteFullyDelegatesToWriteASingleTimeEvenIfBytesHasNotBytesRemaing() throws IOException {
			int count = 0;
			int position = 31;
			ByteBuffer buffer = ByteBuffer.allocate(count);
			when(channel.write(buffer, position)).then(simulateWrite(count, buffer));

			int result = inTest.writeFully(position, buffer);

			assertThat(result, is(count));
			verify(channel).write(buffer, position);
		}

		private Answer<Future<Integer>> simulateWrite(int amount, ByteBuffer target) {
			return new Answer<Future<Integer>>() {
				@Override
				public Future<Integer> answer(InvocationOnMock invocation) throws Throwable {
					target.position(target.position() + amount);
					return constantFuture(amount);
				}
			};
		}

	}

	public class OperationsFailingIfClosed {

		@Test
		public void testReadFullyFailsIfNotOpen() {
			ByteBuffer irrelevant = null;

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel is not open");

			inTest.readFully(0, irrelevant);
		}

		@Test
		public void testTruncateFailsIfNotOpen() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel is not open");

			inTest.truncate(0);
		}

		@Test
		public void testSizeFailsIfNotOpen() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel is not open");

			inTest.size();
		}

		@Test
		public void testWriteFullyFailsIfNotOpen() {
			ByteBuffer irrelevant = null;

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel is not open");

			inTest.writeFully(0, irrelevant);
		}

	}

	private void inThreadRethrowingException(Runnable task) {
		Holder<Throwable> exception = new Holder<>(null);
		Thread thread = new Thread(() -> {
			try {
				task.run();
			} catch (Throwable e) {
				exception.set(e);
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		rethrowUnchecked(exception.get());
	}

	private void rethrowUnchecked(Throwable exception) {
		if (exception instanceof RuntimeException) {
			throw (RuntimeException) exception;
		} else if (exception instanceof Error) {
			throw (Error) exception;
		} else if (exception != null) {
			throw new RuntimeException(exception);
		}
	}

}
