package org.cryptomator.filesystem.nio;

import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.cryptomator.filesystem.nio.PathMatcher.isFile;
import static org.cryptomator.filesystem.nio.SharedFileChannel.EOF;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.ws.Holder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class SharedFileChannelTest {

	private static final byte[] FILE_CONTENT = "FileContent".getBytes();
	private static final byte[] OTHER_FILE_CONTENT = "Other".getBytes();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Path pathOfTestfile;
	private Path pathOfOtherTestfile;

	private SharedFileChannel inTest;
	private SharedFileChannel otherInTest;

	@Before
	public void setup() throws IOException {
		pathOfTestfile = Files.createTempFile("sharedFileChannelTest", "tmp");
		writeByteArrayToFile(pathOfTestfile.toFile(), FILE_CONTENT);
		pathOfOtherTestfile = Files.createTempFile("sharedFileChannelTest", "tmp");
		writeByteArrayToFile(pathOfOtherTestfile.toFile(), OTHER_FILE_CONTENT);

		assertThat(pathOfTestfile, isFile().withContent(FILE_CONTENT));
		assertThat(pathOfOtherTestfile, isFile().withContent(OTHER_FILE_CONTENT));

		inTest = new SharedFileChannel(pathOfTestfile);
		otherInTest = new SharedFileChannel(pathOfOtherTestfile);
	}

	public class ReadFully {

		@Before
		public void setup() {
			inTest.open(OpenMode.READ);
		}

		@Test
		public void testReadFullyReadsFileContents() {
			byte[] result = new byte[FILE_CONTENT.length];
			ByteBuffer buffer = ByteBuffer.wrap(result);

			int read = inTest.readFully(0, buffer);

			assertThat(result, is(FILE_CONTENT));
			assertThat(read, is(FILE_CONTENT.length));
		}

		@Test
		public void testReadFullyReadsFromPositionContents() {
			int position = 5;
			byte[] result = new byte[FILE_CONTENT.length - position];
			ByteBuffer buffer = ByteBuffer.wrap(result);

			int read = inTest.readFully(position, buffer);

			assertThat(result, is(copyOfRange(FILE_CONTENT, position, FILE_CONTENT.length)));
			assertThat(read, is(FILE_CONTENT.length - position));
		}

		@Test
		public void testReadFullyReadsTillEndIfBufferHasMoreSpaceAvailabe() {
			int position = 5;
			byte[] result = new byte[FILE_CONTENT.length];
			ByteBuffer buffer = ByteBuffer.wrap(result);
			byte[] expected = copyOf(copyOfRange(FILE_CONTENT, position, FILE_CONTENT.length), FILE_CONTENT.length);

			int read = inTest.readFully(position, buffer);

			assertThat(result, is(expected));
			assertThat(read, is(FILE_CONTENT.length - position));
		}

		@Test
		public void testReadReadsNothingIfBufferHasNoSpaceAvailabe() {
			ByteBuffer buffer = ByteBuffer.allocate(5);
			buffer.position(5);

			int read = inTest.readFully(0, buffer);

			assertThat(buffer.array(), is(new byte[5]));
			assertThat(read, is(0));
		}

		@Test
		public void testReadReadsNothingIfPositionIsEndOfFile() {
			ByteBuffer buffer = ByteBuffer.allocate(5);

			int read = inTest.readFully(FILE_CONTENT.length, buffer);

			assertThat(buffer.array(), is(new byte[5]));
			assertThat(read, is(EOF));
		}

		@Test
		public void testReadReadsNothingIfAfterEndOfFile() {
			ByteBuffer buffer = ByteBuffer.allocate(5);

			int read = inTest.readFully(FILE_CONTENT.length + 10, buffer);

			assertThat(buffer.array(), is(new byte[5]));
			assertThat(read, is(EOF));
		}

		@Test
		public void testReadThrowsIllegalArgumentExceptionIfPositionIsNegative() {
			thrown.expect(IllegalArgumentException.class);

			inTest.readFully(-1, ByteBuffer.allocate(0));
		}

	}

	public class Truncate {

		@Before
		public void setup() {
			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testTruncateToZeroDeletesFileContent() {
			inTest.truncate(0);
			inTest.close();

			assertThat(pathOfTestfile, isFile().thatIsEmpty());
		}

		@Test
		public void testTruncateToSmallerSizeTruncatesFile() {
			inTest.truncate(5);
			inTest.close();

			assertThat(pathOfTestfile, isFile().withContent(copyOfRange(FILE_CONTENT, 0, 5)));
		}

		@Test
		public void testTruncateToGreaterSizeDoesNotChangeFile() {
			inTest.truncate(FILE_CONTENT.length + 10);
			inTest.close();

			assertThat(pathOfTestfile, isFile().withContent(FILE_CONTENT));
		}
	}

	@Test
	public void testSizeReturnsFileSize() {
		inTest.open(OpenMode.READ);

		assertThat(inTest.size(), is((long) FILE_CONTENT.length));
	}

	public class TransferTo {

		@Before
		public void setup() {
			inTest.open(OpenMode.READ);
			otherInTest.open(OpenMode.WRITE);
		}

		@Test
		public void testTransferToCopiesSelectedRegionToTargetPosition() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToDoesNothingIfCountIsZero() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToTransfersLessThanCountBytesIfEndOfSourceFileIsReached() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToThrowsIllegalArgumentExceptionIfPositionIsSmallerZero() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToThrowsIllegalArgumentExceptionIfCountIsSmallerZero() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToThrowsIllegalArgumentExceptionIfTargetPositionIsSmallerZero() {
			// TODO Markus Kreusch
		}

		@Test
		public void testTransferToTransfersSelectedRegionToTargetAfterEndOfFile() {
			// TODO Markus Kreusch
		}

	}

	public class WriteFully {

		@Before
		public void setup() {
			inTest.open(OpenMode.WRITE);
		}

		@Test
		public void testWriteFullyWritesFileContents() {
			// TODO Markus Kreusch
		}

		@Test
		public void testWriteFullyWritesContentsToPosition() {
			// TODO Markus Kreusch
		}

		@Test
		public void testWriteFullyWritesContentsEvenIfPositionIsGreaterCurrentEndOfFile() {
			// TODO Markus Kreusch
		}

		@Test
		public void testWriteWritesNothingBufferHasNothingAvailabe() {
			// TODO Markus Kreusch
		}

		@Test
		public void testWriteThrowsIllegalArgumentExceptionIfPositionIsNegative() {
			thrown.expect(IllegalArgumentException.class);

			inTest.writeFully(-1, ByteBuffer.allocate(0));
		}

	}

	public class MultipleOpenInvocation {

		@Test
		public void testInvokeOpenMultipleTimesFromSameThreadThrowsException() {
			inTest.open(OpenMode.READ);

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("A thread can only open a SharedFileChannel once");

			inTest.open(OpenMode.READ);
		}

		@Test
		public void testInvokeOpenMultipleTimesFromDifferentThreadsWorks() {
			inTest.open(OpenMode.READ);
			inThreadRethrowingExceptions(() -> inTest.open(OpenMode.READ));
		}

	}

	public class NonOpenChannel {

		@Test
		public void testClosingNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.close();
		}

		@Test
		public void testInvokingReadFullyOnNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.readFully(0, null);
		}

		@Test
		public void testInvokingTruncateOnNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.truncate(0);
		}

		@Test
		public void testInvokingSizeOnNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.size();
		}

		@Test
		public void testInvokingTransferToOnNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.transferTo(0, 1, null, 0);
		}

		@Test
		public void testInvokingTransferToPassingNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.open(OpenMode.READ);

			inTest.transferTo(0, 1, otherInTest, 0);
		}

		@Test
		public void testInvokinWriteFullyOnNonOpenChannelThrowsException() {
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("SharedFileChannel closed for current thread");

			inTest.writeFully(0, null);
		}

	}

	@After
	@SuppressWarnings("deprecation")
	public void teardown() {
		inTest.forceClose();
		otherInTest.forceClose();
		try {
			Files.delete(pathOfTestfile);
		} catch (IOException e) {
			// ignore
		}
		try {
			Files.delete(pathOfOtherTestfile);
		} catch (IOException e) {
			// ignore
		}
	}

	private void inThreadRethrowingExceptions(Runnable task) {
		Holder<RuntimeException> holder = new Holder<>();
		Thread thread = new Thread(() -> {
			try {
				task.run();
			} catch (RuntimeException e) {
				holder.value = e;
			}
		});
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
		if (holder.value != null) {
			throw holder.value;
		}
	}

}
