package org.cryptomator.filesystem.nio;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.contains;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class DefaultNioAccessTest {

	private DefaultNioAccess inTest = new DefaultNioAccess();

	private Path path;
	private Path otherPath;

	private FileSystem fileSystem;
	private FileSystemProvider provider;

	@Before
	public void setUp() {
		path = mock(Path.class);
		otherPath = mock(Path.class);
		fileSystem = mock(FileSystem.class);
		provider = mock(FileSystemProvider.class);

		when(fileSystem.provider()).thenReturn(provider);
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(otherPath.getFileSystem()).thenReturn(fileSystem);
	}

	@Test
	public void testOpenCallsOpenOnProvider() throws IOException {
		OpenOption[] options = {StandardOpenOption.APPEND};
		AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);
		when(provider.newAsynchronousFileChannel(path, new HashSet<>(asList(options)), null, new FileAttribute[0])).thenReturn(channel);

		AsynchronousFileChannel result = inTest.open(path, options);

		assertThat(result, is(channel));
	}

	@Test
	public void testIsRegularFileCallsIsRegularFileOnProvider() throws IOException {
		LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		when(attributes.isRegularFile()).thenReturn(true);
		when(provider.readAttributes(path, BasicFileAttributes.class, options)).thenReturn(attributes);

		boolean result = inTest.isRegularFile(path, options);

		assertThat(result, is(true));
		verify(attributes).isRegularFile();
	}

	@Test
	public void testIsDirectoryCallsIsDirectoryOnProvider() throws IOException {
		LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		when(attributes.isDirectory()).thenReturn(true);
		when(provider.readAttributes(path, BasicFileAttributes.class, options)).thenReturn(attributes);

		boolean result = inTest.isDirectory(path, options);

		assertThat(result, is(true));
		verify(attributes).isDirectory();
	}

	@Test
	public void testExistsCallsProviderCheckAccessAndReturnsTrueIfItWorks() throws IOException {
		boolean result = inTest.exists(path);

		assertThat(result, is(true));
		verify(provider).checkAccess(path);
	}

	@Test
	public void testExistsCallsProviderCheckAccessAndReturnsFalseIfItThrowsAFileNotFoundException() throws IOException {
		doThrow(new FileNotFoundException()).when(provider).checkAccess(path);

		boolean result = inTest.exists(path);

		assertThat(result, is(false));
	}

	@Test
	public void testListReturnsStreamCreatedFromDirectoryStream() throws IOException {
		@SuppressWarnings("unchecked")
		DirectoryStream<Path> directoryStream = mock(DirectoryStream.class);
		Path aPath = mock(Path.class);
		Path anotherPath = mock(Path.class);
		when(directoryStream.iterator()).thenReturn(asList(aPath, anotherPath).iterator());
		when(provider.newDirectoryStream(same(path), any())).thenReturn(directoryStream);

		assertThat(inTest.list(path).collect(toList()), contains(is(aPath), is(anotherPath)));
	}

	@Test
	public void testCreateDirectoriesCreatesDirectoryUsingProvider() throws IOException {
		FileAttribute<?>[] attributes = {mock(FileAttribute.class)};

		inTest.createDirectories(path, attributes);

		verify(provider).createDirectory(path, attributes);
	}

	@Test
	public void testGetLastModifiedTimeGetsLastModifiedTimeUsingProvider() throws IOException {
		LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};
		FileTime expectedResult = FileTime.from(Instant.now());
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		when(attributes.lastModifiedTime()).thenReturn(expectedResult);
		when(provider.readAttributes(path, BasicFileAttributes.class, options)).thenReturn(attributes);

		FileTime result = inTest.getLastModifiedTime(path, options);

		assertThat(result, is(expectedResult));
	}

	@Test
	public void testSetLastModifiedTimeSetsLastModifiedTimeUsingProvider() throws IOException {
		FileTime fileTime = FileTime.from(Instant.now());
		BasicFileAttributeView attributes = mock(BasicFileAttributeView.class);
		when(provider.getFileAttributeView(path, BasicFileAttributeView.class)).thenReturn(attributes);

		inTest.setLastModifiedTime(path, fileTime);

		verify(attributes).setTimes(fileTime, null, null);
	}

	@Test
	public void testDeleteDeletesUsingProvider() throws IOException {
		inTest.delete(path);

		verify(provider).delete(path);
	}

	@Test
	public void testDeleteMovesUsingProvider() throws IOException {
		CopyOption[] options = {ATOMIC_MOVE};
		inTest.move(path, otherPath, options);

		verify(provider).move(path, otherPath, options);
	}

	@Test
	public void testCloseInvokesCloseOnChannel() throws IOException {
		AsynchronousFileChannel channel = mock(AsynchronousFileChannel.class);

		inTest.close(channel);

		verify(channel).close();
	}

	@Test
	public void testGetCreationTimeReadsAttributesUsingProviderAndReturnsValueFromThem() throws IOException {
		FileTime expectedValue = FileTime.from(Instant.parse("2016-01-08T22:32:00Z"));
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		when(attributes.creationTime()).thenReturn(expectedValue);
		LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};
		when(provider.readAttributes(path, BasicFileAttributes.class, options)).thenReturn(attributes);

		FileTime result = inTest.getCreationTime(path, options);

		assertThat(result, is(expectedValue));
	}

	@Test
	public void testSetCreationTimeGetsAttributeViewUsingProviderAndSetsCreationTimeUsingIt() throws IOException {
		FileTime fileTime = FileTime.from(Instant.now());
		BasicFileAttributeView attributes = mock(BasicFileAttributeView.class);
		LinkOption[] options = {LinkOption.NOFOLLOW_LINKS};
		when(provider.getFileAttributeView(path, BasicFileAttributeView.class, options)).thenReturn(attributes);

		inTest.setCreationTime(path, fileTime, options);

		verify(attributes).setTimes(null, null, fileTime);
	}

	@Test
	public void testSeparatorReturnsSeparatorOfDefaultFileSystem() {
		assertThat(inTest.separator(), is(FileSystems.getDefault().getSeparator()));
	}

}
