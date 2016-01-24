package org.cryptomator.filesystem;

import static java.util.Arrays.asList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.contains;
import static org.cryptomator.common.test.mockito.Answers.collectParameters;
import static org.cryptomator.common.test.mockito.Answers.consecutiveAnswers;
import static org.cryptomator.common.test.mockito.Answers.value;
import static org.cryptomator.filesystem.File.EOF;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class CopierTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockitoRule mockitoRule = MockitoJUnit.rule();

	public class CopyFiles {

		@Mock
		private File source;

		@Mock
		private File destination;

		@Mock
		private ReadableFile readable;

		@Mock
		private WritableFile writable;

		@Before
		public void setUp() {
			when(source.openReadable()).thenReturn(readable);
			when(destination.openWritable()).thenReturn(writable);
		}

		@Test
		public void testCopyFileOpensFilesInSortedOrderIfSourceIsSmallerDestination() {
			mockCompareToWithOrder(source, destination);
			when(readable.read(any())).thenReturn(EOF);

			Copier.copy(source, destination);

			InOrder inOrder = inOrder(source, destination);
			inOrder.verify(source).openReadable();
			inOrder.verify(destination).openWritable();
		}

		@Test
		public void testCopyFileOpensFilesInSortedOrderIfDestinationIsSmallerSource() {
			mockCompareToWithOrder(destination, source);
			when(readable.read(any())).thenReturn(EOF);

			Copier.copy(source, destination);

			InOrder inOrder = inOrder(source, destination);
			inOrder.verify(destination).openWritable();
			inOrder.verify(source).openReadable();
		}

		@Test
		public void testCopyFileReadsAndWritesReadableSourceAndWritableDestintationUntilEof() {
			int irrelevantValue = 0;
			Collection<byte[]> written = new ArrayList<>();
			mockCompareToWithOrder(source, destination);
			byte[] read1 = {1, 48, 32, 33, 22};
			byte[] read2 = {4, 3, 1, -2, -8};
			when(readable.read(any())).then(consecutiveAnswers(fillBufferWith(read1), fillBufferWith(read2), value(EOF)));
			when(writable.write(any())).then(collectParameters(value(irrelevantValue), (ByteBuffer buffer) -> {
				byte[] data = new byte[buffer.remaining()];
				buffer.get(data);
				written.add(data);
			}));

			Copier.copy(source, destination);

			InOrder inOrder = inOrder(readable, writable);
			inOrder.verify(writable).truncate();
			inOrder.verify(readable).read(any());
			inOrder.verify(writable).write(any());
			inOrder.verify(readable).read(any());
			inOrder.verify(writable).write(any());
			inOrder.verify(readable).read(any());
			inOrder.verify(readable).close();
			inOrder.verify(writable).close();

			assertThat(written, contains(is(read1), is(read2)));
		}

		private Answer<Integer> fillBufferWith(byte[] data) {
			return new Answer<Integer>() {
				@Override
				public Integer answer(InvocationOnMock invocation) throws Throwable {
					ByteBuffer buffer = invocation.getArgumentAt(0, ByteBuffer.class);
					for (byte value : data) {
						buffer.put(value);
					}
					return data.length;
				}

			};
		}

		private void mockCompareToWithOrder(File first, File last) {
			when(first.compareTo(last)).thenReturn(-1);
			when(last.compareTo(first)).thenReturn(1);
		}

	}

	public class CopyFolders {

		@Mock
		private Folder source;

		@Mock
		private Folder destination;

		@Test
		public void testCopyFolderDeletesAndCreatesDestinationBeforeIteratingOverTheFilesAndFoldersInSource() {
			when(source.files()).thenReturn(Stream.empty());
			when(source.folders()).thenReturn(Stream.empty());

			Copier.copy(source, destination);

			InOrder inOrder = inOrder(source, destination);
			inOrder.verify(destination).delete();
			inOrder.verify(destination).create();
			inOrder.verify(source).files();
			inOrder.verify(source).folders();
		}

		@Test
		@SuppressWarnings({"unchecked", "rawtypes"})
		public void testCopyFolderInvokesCopyToOnAllFilesInSourceWithFileWithSameNameFromDestination() {
			String filename1 = "nameOfFile1";
			String filename2 = "nameOfFile2";
			File file1 = mock(File.class);
			File file2 = mock(File.class);
			File destinationFile1 = mock(File.class);
			File destinationFile2 = mock(File.class);
			when(source.files()).thenReturn((Stream) asList(file1, file2).stream());
			when(source.folders()).thenReturn(Stream.empty());
			when(destination.file(filename1)).thenReturn(destinationFile1);
			when(destination.file(filename2)).thenReturn(destinationFile2);
			when(file1.name()).thenReturn(filename1);
			when(file2.name()).thenReturn(filename2);

			Copier.copy(source, destination);

			verify(file1).copyTo(destinationFile1);
			verify(file2).copyTo(destinationFile2);
		}

		@Test
		@SuppressWarnings({"unchecked", "rawtypes"})
		public void testCopyFolderInvokesCopyToOnAllFoldersInSourceWithFolderWithSameNameFromDestination() {
			String folderName1 = "nameOfFolder1";
			String folderName2 = "nameOfFolder2";
			Folder folder1 = mock(Folder.class);
			Folder folder2 = mock(Folder.class);
			Folder destinationfolder1 = mock(Folder.class);
			Folder destinationfolder2 = mock(Folder.class);
			when(source.folders()).thenReturn((Stream) asList(folder1, folder2).stream());
			when(source.files()).thenReturn(Stream.empty());
			when(destination.folder(folderName1)).thenReturn(destinationfolder1);
			when(destination.folder(folderName2)).thenReturn(destinationfolder2);
			when(folder1.name()).thenReturn(folderName1);
			when(folder2.name()).thenReturn(folderName2);

			Copier.copy(source, destination);

			verify(folder1).copyTo(destinationfolder1);
			verify(folder2).copyTo(destinationfolder2);
		}

		@Test
		public void testCopyFolderFailsWithIllegalArgumentExceptionIfSourceIsNestedInDestination() {
			when(source.isAncestorOf(destination)).thenReturn(false);
			when(destination.isAncestorOf(source)).thenReturn(true);

			thrown.expect(IllegalArgumentException.class);

			Copier.copy(source, destination);
		}

		@Test
		public void testCopyFolderFailsWithIllegalArgumentExceptionIfDestinationIsNestedInSource() {
			when(source.isAncestorOf(destination)).thenReturn(true);
			when(destination.isAncestorOf(source)).thenReturn(false);

			thrown.expect(IllegalArgumentException.class);

			Copier.copy(source, destination);
		}

	}

}
