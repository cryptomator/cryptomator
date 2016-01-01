package org.cryptomator.filesystem.nio;

import static org.cryptomator.filesystem.nio.OpenMode.WRITE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
@SuppressWarnings("resource")
public class WritableNioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private NioFile file;

	private SharedFileChannel channel;

	private ReadWriteLock lock;

	private Lock writeLock;

	@Before
	public void setup() {
		file = mock(NioFile.class);
		channel = mock(SharedFileChannel.class);
		lock = mock(ReadWriteLock.class);
		writeLock = mock(Lock.class);

		when(file.channel()).thenReturn(channel);
		when(file.lock()).thenReturn(lock);
		when(lock.writeLock()).thenReturn(writeLock);
	}

	@Test
	public void testWriteFailsIfClosed() {
		WritableNioFile inTest = new WritableNioFile(file);
		inTest.close();
		ByteBuffer irrelevant = null;

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.write(irrelevant);
	}

	@Test
	public void testPositionFailsIfClosed() {
		WritableNioFile inTest = new WritableNioFile(file);
		inTest.close();
		long irrelevant = 1023;

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage("already closed");

		inTest.position(irrelevant);
	}

	@Test
	public void testIsOpenReturnsTrueForNewInstance() {
		WritableNioFile inTest = new WritableNioFile(file);

		assertThat(inTest.isOpen(), is(true));
	}

	@Test
	public void testIsOpenReturnsFalseForClosedInstance() {
		WritableNioFile inTest = new WritableNioFile(file);
		inTest.close();

		assertThat(inTest.isOpen(), is(false));
	}

	@Test
	public void testWriteInvokesChannelsOpenWithModeWriteIfInvokedForTheFirstTimeBeforeInvokingWriteFully() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer irrelevant = null;

		inTest.write(irrelevant);

		InOrder inOrder = inOrder(channel);
		inOrder.verify(channel).open(WRITE);
		inOrder.verify(channel).writeFully(0, irrelevant);
	}

	@Test
	public void testWriteDoesNotModifyBuffer() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.write(buffer);

		verifyZeroInteractions(buffer);
	}

	@Test
	public void testWriteInvokesWriteFullyWithZeroPositionIfNotSet() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);

		inTest.write(buffer);

		verify(channel).writeFully(0, buffer);
	}

	@Test
	public void testWriteInvokesWriteFullyWithSetPosition() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		long position = 10;
		inTest.position(position);

		inTest.write(buffer);

		verify(channel).writeFully(position, buffer);
	}

	@Test
	public void testWriteInvokesWriteFullyWithEndOfPreviousWriteIfInvokedTwice() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		int endOfPreviousWrite = 10;
		when(channel.writeFully(0, buffer)).thenReturn(endOfPreviousWrite);

		inTest.write(buffer);
		inTest.write(buffer);

		verify(channel).writeFully(endOfPreviousWrite, buffer);
	}

	@Test
	public void testWriteReturnsResultOfWriteFully() {
		WritableNioFile inTest = new WritableNioFile(file);
		ByteBuffer buffer = mock(ByteBuffer.class);
		int resultOfWriteFully = 14;
		when(channel.writeFully(0, buffer)).thenReturn(resultOfWriteFully);

		int result = inTest.write(buffer);

		assertThat(result, is(resultOfWriteFully));
	}

	@Test
	public void testToString() {
		String fileToString = file.toString();
		WritableNioFile inTest = new WritableNioFile(file);

		assertThat(inTest.toString(), is("Writable" + fileToString));
	}

}
