package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.cryptomator.common.test.matcher.OptionalMatcher.emptyOptional;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class NioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private NioFileSystem fileSystem;

	private Optional<NioFolder> parent;

	private Path path;
	private NioAccess nioAccess;
	private InstanceFactory instanceFactory;

	private SharedFileChannel channel;

	private NioFile inTest;

	@Before
	public void setUp() {
		fileSystem = mock(NioFileSystem.class);
		path = mock(Path.class);
		nioAccess = mock(NioAccess.class);
		instanceFactory = mock(InstanceFactory.class);
		channel = mock(SharedFileChannel.class);
		parent = Optional.of(fileSystem);

		Path maybeNonAbsolutePath = mock(Path.class);
		when(maybeNonAbsolutePath.toAbsolutePath()).thenReturn(path);

		when(fileSystem.fileSystem()).thenReturn(fileSystem);
		when(instanceFactory.sharedFileChannel(path, nioAccess)).thenReturn(channel);

		inTest = new NioFile(parent, maybeNonAbsolutePath, nioAccess, instanceFactory);
	}

	public class Constructor {

		@Test
		public void testConstructorCreatesASharedFileChannelFromAbsolutePathAndNioAccessUsingTheInstanceFactory() {
			verify(instanceFactory).sharedFileChannel(path, nioAccess);
		}

		@Test
		public void testConstructorSetsParentPassedToIt() {
			assertThat(inTest.parent(), is(parent));
		}

	}

	public class Open {

		@Test
		public void testOpenReadableCreatesReadableNioFileFromNioFile() {
			ReadableNioFile readableNioFile = mock(ReadableNioFile.class);
			when(instanceFactory.readableNioFile(same(path), same(channel), any())).thenReturn(readableNioFile);

			ReadableFile readableFile = inTest.openReadable();

			assertThat(readableFile, is(readableNioFile));
		}

		@Test
		public void testOpenReadableInvokedBeforeInvokingAfterCloseOperationThrowsIllegalStateException() {
			inTest.openReadable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("already reading this file");

			inTest.openReadable();
		}

		@Test
		public void testOpenReadableInvokedAfterAfterCloseOperationCreatesNewReadableFile() {
			ReadableNioFile readableNioFile = mock(ReadableNioFile.class);
			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			when(instanceFactory.readableNioFile(same(path), same(channel), captor.capture())).thenReturn(null, readableNioFile);
			inTest.openReadable();
			captor.getValue().run();

			ReadableFile readableFile = inTest.openReadable();

			assertThat(readableFile, is(readableNioFile));
		}

		@Test
		public void testOpenReadableInvokedBeforeInvokingAfterCloseOperationOfOpenWritableThrowsIllegalStateException() {
			inTest.openWritable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("currently writing this file");

			inTest.openReadable();
		}

		@Test
		public void testOpenReadableInvokedAfterInvokingAfterCloseOperationWorks() {
			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			when(instanceFactory.writableNioFile(same(fileSystem), same(path), same(channel), captor.capture())).thenReturn(null);
			inTest.openWritable();
			captor.getValue().run();

			inTest.openReadable();
		}

		@Test
		public void testOpenWritableCreatesWritableNioFileFromNioFileAndNioAccessUsingInstanceFactory() {
			WritableNioFile writableNioFile = mock(WritableNioFile.class);
			when(instanceFactory.writableNioFile(same(fileSystem), same(path), same(channel), any())).thenReturn(writableNioFile);

			WritableFile writableFile = inTest.openWritable();

			assertThat(writableFile, is(writableNioFile));
		}

		@Test
		public void testOpenWritableInvokedAfterAfterCloseOperationCreatesNewWritableFile() {
			WritableNioFile writableNioFile = mock(WritableNioFile.class);
			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			when(instanceFactory.writableNioFile(same(fileSystem), same(path), same(channel), captor.capture())).thenReturn(null, writableNioFile);
			inTest.openWritable();
			captor.getValue().run();

			WritableFile writableFile = inTest.openWritable();

			assertThat(writableFile, is(writableNioFile));
		}

		@Test
		public void testOpenWritableInvokedBeforeInvokingAfterCloseOperationThrowsIllegalStateException() {
			inTest.openWritable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("already writing this file");

			inTest.openWritable();
		}

		@Test
		public void testOpenWritableInvokedBeforeInvokingAfterCloseOperationFromOpenReadableThrowsIllegalStateException() {
			inTest.openReadable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("currently reading this file");

			inTest.openWritable();
		}

		@Test
		public void testOpenWritableInvokedAfterInvokingAfterCloseOperationWorks() {
			ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
			when(instanceFactory.readableNioFile(same(path), same(channel), captor.capture())).thenReturn(null);
			inTest.openReadable();
			captor.getValue().run();

			inTest.openWritable();
		}

	}

	public class MoveTo {

		private NioFile target;

		private Path pathOfTarget;

		@Before
		public void setUp() {
			target = mock(NioFile.class);
			pathOfTarget = mock(Path.class);
			when(target.fileSystem()).thenReturn(fileSystem);
			when(target.path()).thenReturn(pathOfTarget);
		}

		@Test
		public void testMoveToFailsIfTargetIsNoWritableNioFile() {
			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage("Can only move to a File from the same FileSystem");

			inTest.moveTo(mock(File.class));
		}

		@Test
		public void testMoveToFailsIfTargetIsNotFromSameFileSystem() {
			NioFile targetFromOtherFileSystem = mock(NioFile.class);
			when(targetFromOtherFileSystem.fileSystem()).thenReturn(mock(FileSystem.class));

			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage("Can only move to a File from the same FileSystem");

			inTest.moveTo(targetFromOtherFileSystem);
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
		public void testMoveToWrapsIOExceptionFromMoveInUncheckedIOException() throws IOException {
			when(nioAccess.isDirectory(path)).thenReturn(false);
			when(nioAccess.isDirectory(pathOfTarget)).thenReturn(false);
			IOException exceptionFromMove = new IOException();
			doThrow(exceptionFromMove).when(nioAccess).move(path, pathOfTarget, REPLACE_EXISTING);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromMove));

			inTest.moveTo(target);
		}

	}

	public class Exists {

		@Test
		public void testExistsReturnsTrueIfPathIsRegularFile() {
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			assertThat(inTest.exists(), is(true));
		}

		@Test
		public void testExistsReturnsFalseIfPathIsntRegularFile() {
			when(nioAccess.isRegularFile(path)).thenReturn(false);

			assertThat(inTest.exists(), is(false));
		}

	}

	public class LastModified {

		@Test
		public void testLastModifiedReturnsLastModifiedTimeForExistingFile() throws IOException {
			Instant instant = Instant.parse("2016-01-04T01:24:32Z");
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			when(nioAccess.getLastModifiedTime(path)).thenReturn(FileTime.from(instant));

			Instant result = inTest.lastModified();

			assertThat(result, is(instant));
		}

		@Test
		public void testLastModifiedInvokesGetLastModifiedTimeForNonExistingFile() throws IOException {
			Instant instant = Instant.parse("2016-01-04T01:24:32Z");
			when(nioAccess.exists(path)).thenReturn(false);
			when(nioAccess.getLastModifiedTime(path)).thenReturn(FileTime.from(instant));

			Instant result = inTest.lastModified();

			assertThat(result, is(instant));
		}

		@Test
		public void testLastModifiedWrapsIOExceptionThrownByGetLastModifiedTimeInUncheckedIOException() throws IOException {
			IOException exceptionThrownFromGetLastModifiedTime = new IOException();
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(true);
			when(nioAccess.getLastModifiedTime(path)).thenThrow(exceptionThrownFromGetLastModifiedTime);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionThrownFromGetLastModifiedTime));

			inTest.lastModified();
		}

		@Test
		public void testLastModifiedThrowsUncheckedIOExceptionIfPathExistsButIsNoRegularFile() {
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(false);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s is a folder", path));

			inTest.lastModified();
		}

	}

	public class SetLastModified {

		@Test
		public void testSetLastModifiedOpensChannelIfClosedAndSetsLastModifiedTime() throws IOException {
			Instant instant = Instant.parse("2016-01-05T13:51:00Z");
			FileTime time = FileTime.from(instant);

			inTest.setLastModified(instant);

			InOrder inOrder = inOrder(channel, nioAccess);
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

	public class CreationTime {

		@Test
		public void testCreationTimeDelegatesToNioAccessCreationTime() throws IOException {
			Instant exectedResult = Instant.parse("1970-01-02T00:00:00Z");
			when(nioAccess.getCreationTime(path)).thenReturn(FileTime.from(exectedResult));
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			Instant result = inTest.creationTime().get();

			assertThat(result, is(exectedResult));
		}

		@Test
		public void testCreationTimeReturnsEmptyOptionalIfNioAccessCreationTimeReturnsValueBeforeJanuaryTheSecondNineteenhundredSeventy() throws IOException {
			when(nioAccess.getCreationTime(path)).thenReturn(FileTime.from(Instant.parse("1970-01-01T23:59:59Z")));
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			assertThat(inTest.creationTime(), is(emptyOptional()));
		}

		@Test
		public void testCreationTimeWrapsIOExceptionFromNioAccessCreationTimeInUncheckedIOException() throws IOException {
			IOException exceptionFromCreationTime = new IOException();
			when(nioAccess.getCreationTime(path)).thenThrow(exceptionFromCreationTime);
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromCreationTime));

			inTest.creationTime();
		}

		@Test
		public void testCreationTimeThrowsExceptionIfFileIsNoRegularFile() {
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isRegularFile(path)).thenReturn(false);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s is a folder", path));

			inTest.creationTime();
		}

	}

	public class SetCreationTime {

		@Test
		public void testSetCreationTimeOpensChannelIfClosedAndInvokesNioAccessSetCreationTimeAfterwards() throws IOException {
			Instant instant = Instant.parse("2016-01-08T22:32:00Z");

			inTest.setCreationTime(instant);

			InOrder inOrder = inOrder(nioAccess, channel);
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

	public class CompareTo {

		private Path otherPath;
		private NioFile otherInTest;

		@Before
		public void setUp() {
			otherPath = mock(Path.class);

			Path maybeNonAbsolutePath = mock(Path.class);
			when(maybeNonAbsolutePath.toAbsolutePath()).thenReturn(otherPath);

			otherInTest = new NioFile(parent, maybeNonAbsolutePath, nioAccess, instanceFactory);
		}

		@Test
		public void testCompareToFileFromOtherFileSystemThrowsIllegalArgumentException() {
			File other = mock(File.class);
			when(other.fileSystem()).thenReturn(mock(FileSystem.class));

			thrown.expect(IllegalArgumentException.class);

			inTest.compareTo(other);
		}

		@Test
		public void testCompareToReturnsResultOfPathsCompareTo() {
			int expectedResult = 2873;
			when(path.compareTo(otherPath)).thenReturn(expectedResult);

			int result = inTest.compareTo(otherInTest);

			assertThat(result, is(expectedResult));
		}

	}

	@Test
	public void testNameReturnsFileNameOfPath() {
		Path fileName = mock(Path.class);
		when(path.getFileName()).thenReturn(fileName);

		String name = inTest.name();

		assertThat(name, is(fileName.toString()));
	}

	@Test
	public void testToString() {
		assertThat(inTest.toString(), is(format("NioFile(%s)", path)));
	}

}