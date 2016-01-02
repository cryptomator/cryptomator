package org.cryptomator.filesystem.nio;

import static org.cryptomator.filesystem.nio.OpenMode.READ;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
@SuppressWarnings("resource")
public class ReadableNioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private NioFile file;

	private SharedFileChannel channel;

	private ReadWriteLock lock;

	private Lock readLock;

	@Before
	public void setup() {
		file = mock(NioFile.class);
		channel = mock(SharedFileChannel.class);
		lock = mock(ReadWriteLock.class);
		readLock = mock(Lock.class);

		when(file.channel()).thenReturn(channel);
		when(file.lock()).thenReturn(lock);
		when(lock.readLock()).thenReturn(readLock);
	}

	@Test
	public void testConstructorInvokesOpenWithReadModeOnChannelOfNioFile() {
		new ReadableNioFile(file);

		verify(channel).open(READ);
	}

	@Test
	public void testReadFailsIfClosed() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer irrelevant = null;
		inTest.close();

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.read(irrelevant);
	}

	@Test
	public void testPositionFailsIfClosed() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		int irrelevant = 1;
		inTest.close();

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.position(irrelevant);
	}

	@Test
	public void testPositionFailsIfNegative() {
		ReadableNioFile inTest = new ReadableNioFile(file);

		thrown.expect(IllegalArgumentException.class);

		inTest.position(-1);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithZeroPositionIfNotSet() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.read(buffer);

		verify(channel).readFully(0, buffer);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithPositionAtEndOfPreviousReadIfInvokedTwice() {
		ReadableNioFile inTest = new ReadableNioFile(file);
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
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		when(channel.readFully(0, buffer)).thenReturn(SharedFileChannel.EOF);

		inTest.read(buffer);
		inTest.read(buffer);

		verify(channel, times(2)).readFully(0, buffer);
	}

	@Test
	public void testReadDelegatesToChannelReadFullyWithSetPosition() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		int position = 10;
		inTest.position(position);

		inTest.read(buffer);

		verify(channel).readFully(position, buffer);
	}

	@Test
	public void testReadReturnsValueOfChannelReadFully() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		int expectedResult = 37028;
		when(channel.readFully(0, buffer)).thenReturn(expectedResult);

		int result = inTest.read(buffer);

		assertThat(result, is(expectedResult));
	}

	@Test
	public void testReadDoesNotModifyBuffer() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.read(buffer);

		verifyZeroInteractions(buffer);
	}

	public class CopyTo {

		@Mock
		private NioFile otherFile;

		@Mock
		private WritableNioFile writableOtherFile;

		@Mock
		private SharedFileChannel otherChannel;

		@Before
		public void setup() {
			otherFile = mock(NioFile.class);
			writableOtherFile = mock(WritableNioFile.class);
			otherChannel = mock(SharedFileChannel.class);

			when(writableOtherFile.nioFile()).thenReturn(otherFile);
			when(writableOtherFile.channel()).thenReturn(otherChannel);
		}

		@Test
		public void testCopyToFailsIfTargetBelongsToOtherFileSystem() {
			ReadableNioFile inTest = new ReadableNioFile(file);
			when(otherFile.belongsToSameFilesystem(file)).thenReturn(false);

			thrown.expect(IllegalArgumentException.class);

			inTest.copyTo(writableOtherFile);
		}

		@Test
		public void testCopyToFailsIfSourceIsClosed() {
			ReadableNioFile inTest = new ReadableNioFile(file);
			when(otherFile.belongsToSameFilesystem(file)).thenReturn(true);
			inTest.close();

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage("already closed");

			inTest.copyTo(writableOtherFile);
		}

		@Test
		public void testCopyToAssertsThatTargetIsOpenEnsuresTargetChannelIsOpenTuncatesItAndTransfersDataFromSourceChannel() {
			ReadableNioFile inTest = new ReadableNioFile(file);
			when(otherFile.belongsToSameFilesystem(file)).thenReturn(true);
			long sizeOfSourceChannel = 3283;
			when(channel.size()).thenReturn(sizeOfSourceChannel);
			when(channel.transferTo(0, sizeOfSourceChannel, otherChannel, 0)).thenReturn(sizeOfSourceChannel);

			inTest.copyTo(writableOtherFile);

			InOrder inOrder = inOrder(writableOtherFile, otherChannel, channel);
			inOrder.verify(writableOtherFile).assertOpen();
			inOrder.verify(writableOtherFile).ensureChannelIsOpened();
			inOrder.verify(otherChannel).truncate(0);
			inOrder.verify(channel).transferTo(0, sizeOfSourceChannel, otherChannel, 0);
		}

		@Test
		public void testCopyToInvokesTransferToUntilAllBytesHaveBeenTransferred() {
			ReadableNioFile inTest = new ReadableNioFile(file);
			when(otherFile.belongsToSameFilesystem(file)).thenReturn(true);
			long firstTransferAmount = 100;
			long secondTransferAmount = 300;
			long thirdTransferAmount = 500;
			long sizeRemainingAfterSecondTransfer = thirdTransferAmount;
			long sizeRemainingAfterFirstTransfer = sizeRemainingAfterSecondTransfer + secondTransferAmount;
			long size = sizeRemainingAfterFirstTransfer + firstTransferAmount;
			when(channel.size()).thenReturn(size);
			when(channel.transferTo(0, size, otherChannel, 0)).thenReturn(firstTransferAmount);
			when(channel.transferTo(firstTransferAmount, sizeRemainingAfterFirstTransfer, otherChannel, firstTransferAmount)).thenReturn(secondTransferAmount);
			when(channel.transferTo(firstTransferAmount + secondTransferAmount, sizeRemainingAfterSecondTransfer, otherChannel, firstTransferAmount + secondTransferAmount)).thenReturn(thirdTransferAmount);

			inTest.copyTo(writableOtherFile);

			InOrder inOrder = inOrder(channel);
			inOrder.verify(channel).transferTo(0, size, otherChannel, 0);
			inOrder.verify(channel).transferTo(firstTransferAmount, sizeRemainingAfterFirstTransfer, otherChannel, firstTransferAmount);
			inOrder.verify(channel).transferTo(firstTransferAmount + secondTransferAmount, sizeRemainingAfterSecondTransfer, otherChannel, firstTransferAmount + secondTransferAmount);
		}

	}

	@Test
	public void testIsOpenReturnsTrueForNewReadableNioFile() {
		ReadableNioFile inTest = new ReadableNioFile(file);

		assertThat(inTest.isOpen(), is(true));
	}

	@Test
	public void testIsOpenReturnsFalseForClosed() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		inTest.close();

		assertThat(inTest.isOpen(), is(false));
	}

	@Test
	public void testCloseClosesChannelAndUnlocksReadLock() {
		ReadableNioFile inTest = new ReadableNioFile(file);

		inTest.close();

		InOrder inOrder = Mockito.inOrder(channel, readLock);
		inOrder.verify(channel).close();
		inOrder.verify(readLock).unlock();
	}

	@Test
	public void testCloseClosesChannelAndUnlocksReadLockOnlyOnceIfInvokedTwice() {
		ReadableNioFile inTest = new ReadableNioFile(file);

		inTest.close();
		inTest.close();

		InOrder inOrder = Mockito.inOrder(channel, readLock);
		inOrder.verify(channel).close();
		inOrder.verify(readLock).unlock();
	}

	@Test
	public void testCloseUnlocksReadLockEvenIfCloseFails() {
		ReadableNioFile inTest = new ReadableNioFile(file);
		String message = "exceptionMessage";
		doThrow(new RuntimeException(message)).when(channel).close();

		thrown.expectMessage(message);

		try {
			inTest.close();
		} finally {
			verify(readLock).unlock();
		}
	}

	@Test
	public void testToString() {
		String nioFileToString = file.toString();
		ReadableNioFile inTest = new ReadableNioFile(file);

		assertThat(inTest.toString(), is("Readable" + nioFileToString));
	}

}
