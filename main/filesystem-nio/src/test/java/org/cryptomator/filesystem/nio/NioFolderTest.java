package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.contains;
import static org.cryptomator.filesystem.nio.ReflectiveClassMatchers.aClassThatDoesDeclareMethod;
import static org.cryptomator.filesystem.nio.ReflectiveClassMatchers.aClassThatDoesNotDeclareMethod;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.theInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.cryptomator.common.streams.AutoClosingStream;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class NioFolderTest {

	private static final String SEPARATOR = "/";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private NioFileSystem fileSystem;
	private Optional<NioFolder> parent;

	private Path path;
	private NioAccess nioAccess;
	private InstanceFactory instanceFactory;

	private NioFolder inTest;

	@Before
	public void setUp() {
		path = mock(Path.class);
		nioAccess = mock(NioAccess.class);
		instanceFactory = mock(InstanceFactory.class);
		fileSystem = mock(NioFileSystem.class);
		parent = Optional.of(fileSystem);

		Path maybeNonAbsolutePath = mock(Path.class);
		when(maybeNonAbsolutePath.toAbsolutePath()).thenReturn(path);

		when(parent.get().fileSystem()).thenReturn(fileSystem);

		when(nioAccess.separator()).thenReturn(SEPARATOR);

		inTest = new NioFolder(parent, maybeNonAbsolutePath, nioAccess, instanceFactory);
	}

	public class ChildrenTests {

		@Test
		public void testChildrenReturnsAnAutoClosingStream() throws IOException {
			Stream<Path> childrenPaths = Stream.<Path>builder().build();
			when(nioAccess.list(path)).thenReturn(childrenPaths);

			assertThat(inTest.children(), is(instanceOf(AutoClosingStream.class)));
		}

		@Test
		public void testChildrenConvertsPathWhichIsADirectoryToAnNioFolderUsingTheInstanceFactory() throws IOException {
			Path childFolderPath = mock(Path.class);
			NioFolder nioFolderCreatedFromChildFolderPath = mock(NioFolder.class);
			Stream<Path> childrenPaths = Stream.<Path>builder().add(childFolderPath).build();
			when(nioAccess.isDirectory(childFolderPath)).thenReturn(true);
			when(nioAccess.list(path)).thenReturn(childrenPaths);
			when(instanceFactory.nioFolder(Optional.of(inTest), childFolderPath, nioAccess)).thenReturn(nioFolderCreatedFromChildFolderPath);

			assertThat(inTest.children().collect(toList()), contains(theInstance(nioFolderCreatedFromChildFolderPath)));
		}

		@Test
		public void testChildrenConvertsPathWhichIsAFileToAnNioFileUsingTheInstanceFactory() throws IOException {
			Path childFilePath = mock(Path.class);
			NioFile nioFileCreatedFromChildFolderPath = mock(NioFile.class);
			Stream<Path> childrenPaths = Stream.<Path>builder().add(childFilePath).build();
			when(nioAccess.isDirectory(childFilePath)).thenReturn(false);
			when(nioAccess.list(path)).thenReturn(childrenPaths);
			when(instanceFactory.nioFile(Optional.of(inTest), childFilePath, nioAccess)).thenReturn(nioFileCreatedFromChildFolderPath);

			assertThat(inTest.children().collect(toList()), contains(theInstance(nioFileCreatedFromChildFolderPath)));
		}

		@Test
		public void testChildrenConvertsAllPathsToFilesAndFolders() throws IOException {
			Path childFilePath = mock(Path.class);
			Path childFolderPath = mock(Path.class);
			Path anotherChildFilePath = mock(Path.class);
			Path anotherChildFolderPath = mock(Path.class);
			NioFile nioFileCreatedFromChildFilePath = mock(NioFile.class);
			NioFile anotherNioFileCreatedFromChildFilePath = mock(NioFile.class);
			NioFolder nioFolderCreatedFromChildFolderPath = mock(NioFolder.class);
			NioFolder anotherNioFolderCreatedFromChildFolderPath = mock(NioFolder.class);
			Stream<Path> childrenPaths = Stream.<Path>builder() //
					.add(childFilePath) // NioFolder
					.add(childFolderPath) //
					.add(anotherChildFilePath) //
					.add(anotherChildFolderPath).build();
			when(nioAccess.isDirectory(childFilePath)).thenReturn(false);
			when(nioAccess.isDirectory(childFolderPath)).thenReturn(true);
			when(nioAccess.isDirectory(anotherChildFilePath)).thenReturn(false);
			when(nioAccess.isDirectory(anotherChildFolderPath)).thenReturn(true);
			when(nioAccess.list(path)).thenReturn(childrenPaths);
			when(instanceFactory.nioFile(Optional.of(inTest), childFilePath, nioAccess)).thenReturn(nioFileCreatedFromChildFilePath);
			when(instanceFactory.nioFolder(Optional.of(inTest), childFolderPath, nioAccess)).thenReturn(nioFolderCreatedFromChildFolderPath);
			when(instanceFactory.nioFile(Optional.of(inTest), anotherChildFilePath, nioAccess)).thenReturn(anotherNioFileCreatedFromChildFilePath);
			when(instanceFactory.nioFolder(Optional.of(inTest), anotherChildFolderPath, nioAccess)).thenReturn(anotherNioFolderCreatedFromChildFolderPath);

			assertThat(inTest.children().collect(toList()),
					contains( //
							theInstance(nioFileCreatedFromChildFilePath), //
							theInstance(nioFolderCreatedFromChildFolderPath), //
							theInstance(anotherNioFileCreatedFromChildFilePath), //
							theInstance(anotherNioFolderCreatedFromChildFolderPath)));
		}

		@Test
		public void testFilesIsNotOverwritten() {
			assertThat(Folder.class, aClassThatDoesDeclareMethod("files"));
			assertThat(NioFolder.class, aClassThatDoesNotDeclareMethod("files"));
		}

		@Test
		public void testFoldersIsNotOverwritten() {
			assertThat(Folder.class, aClassThatDoesDeclareMethod("folders"));
			assertThat(NioFolder.class, aClassThatDoesNotDeclareMethod("folders"));
		}

		@Test
		public void testChildrenWrapsIOExceptionFromListInUncheckedIOException() throws IOException {
			IOException exceptionFromList = new IOException();
			when(nioAccess.list(path)).thenThrow(exceptionFromList);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromList));

			inTest.children();
		}

	}

	public class FileTests {

		@Test
		public void testFileResolvesTheNameAgainstThePathAndCreatesAnNioFileUsingTheInstanceFactory() {
			String name = "theFileName";
			Path resolvedPath = mock(Path.class);
			NioFile fileCreatedByInstanceFactory = mock(NioFile.class);
			when(path.resolve(name)).thenReturn(resolvedPath);
			when(instanceFactory.nioFile(Optional.of(inTest), resolvedPath, nioAccess)).thenReturn(fileCreatedByInstanceFactory);

			File result = inTest.file(name);

			assertThat(result, is(fileCreatedByInstanceFactory));
		}

		@Test
		public void testSecondInvocationOfFileReturnsChachedResult() {
			String name = "theFileName";
			Path resolvedPath = mock(Path.class);
			NioFile fileCreatedByInstanceFactory = mock(NioFile.class);
			when(path.resolve(name)).thenReturn(resolvedPath);
			when(instanceFactory.nioFile(Optional.of(inTest), resolvedPath, nioAccess)).thenReturn(fileCreatedByInstanceFactory);

			File result = inTest.file(name);
			File secondResult = inTest.file(name);

			assertThat(result, is(fileCreatedByInstanceFactory));
			assertThat(secondResult, is(fileCreatedByInstanceFactory));
			verify(instanceFactory).nioFile(Optional.of(inTest), resolvedPath, nioAccess);
		}

		@Test
		public void testFileInvokedWithNameContainingSeparatorThrowsIllegalArgumentException() {
			String name = "nameContaining" + SEPARATOR;

			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage(name);

			inTest.file(name);
		}

		@Test
		public void testResolveFileIsNotOverwritten() {
			assertThat(Folder.class, is(aClassThatDoesDeclareMethod("resolveFile", String.class)));
			assertThat(NioFolder.class, is(aClassThatDoesNotDeclareMethod("resolveFile", String.class)));
		}

	}

	public class FolderTests {

		@Test
		public void testFolderResolvesTheNameAgainstThePathAndCreatesAnNioFolderUsingTheInstanceFactory() {
			String name = "theFolderName";
			Path resolvedPath = mock(Path.class);
			NioFolder folderCreatedByInstanceFactory = mock(NioFolder.class);
			when(path.resolve(name)).thenReturn(resolvedPath);
			when(instanceFactory.nioFolder(Optional.of(inTest), resolvedPath, nioAccess)).thenReturn(folderCreatedByInstanceFactory);

			Folder result = inTest.folder(name);

			assertThat(result, is(folderCreatedByInstanceFactory));
		}

		@Test
		public void testSecondInvocationOfFileReturnsChachedResult() {
			String name = "theFolderName";
			Path resolvedPath = mock(Path.class);
			NioFolder folderCreatedByInstanceFactory = mock(NioFolder.class);
			when(path.resolve(name)).thenReturn(resolvedPath);
			when(instanceFactory.nioFolder(Optional.of(inTest), resolvedPath, nioAccess)).thenReturn(folderCreatedByInstanceFactory);

			Folder result = inTest.folder(name);
			Folder secondResult = inTest.folder(name);

			assertThat(result, is(folderCreatedByInstanceFactory));
			assertThat(secondResult, is(folderCreatedByInstanceFactory));
			verify(instanceFactory).nioFolder(Optional.of(inTest), resolvedPath, nioAccess);
		}

		@Test
		public void testFolderInvokedWithNameContainingSeparatorThrowsIllegalArgumentException() {
			String name = "nameContaining" + SEPARATOR;

			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage(name);

			inTest.folder(name);
		}

		@Test
		public void testResolveFolderIsNotOverwritten() {
			assertThat(Folder.class, is(aClassThatDoesDeclareMethod("resolveFolder", String.class)));
			assertThat(NioFolder.class, is(aClassThatDoesNotDeclareMethod("resolveFolder", String.class)));
		}

	}

	public class CreateTests {

		@Test
		public void testCreateDelegatesToNioAccessCreateDirectories() throws IOException {
			inTest.create();

			verify(nioAccess).createDirectories(path);
		}

		@Test
		public void testWrapesIOExceptionThrownByCreateDirectoriesInUncheckedIOException() throws IOException {
			IOException exceptionFromCreateDirectories = new IOException();
			doThrow(exceptionFromCreateDirectories).when(nioAccess).createDirectories(path);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromCreateDirectories));

			inTest.create();
		}

	}

	public class LastModifiedTests {

		@Test
		public void testLastModifiedReturnsLastModifiedTimeForExistingFolder() throws IOException {
			Instant instant = Instant.parse("2016-01-04T01:24:32Z");
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isDirectory(path)).thenReturn(true);
			when(nioAccess.getLastModifiedTime(path)).thenReturn(FileTime.from(instant));

			Instant result = inTest.lastModified();

			assertThat(result, is(instant));
		}

		@Test
		public void testLastModifiedInvokesGetLastModifiedTimeForNonExistingFolder() throws IOException {
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
			when(nioAccess.isDirectory(path)).thenReturn(true);
			when(nioAccess.getLastModifiedTime(path)).thenThrow(exceptionThrownFromGetLastModifiedTime);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionThrownFromGetLastModifiedTime));

			inTest.lastModified();
		}

		@Test
		public void testLastModifiedThrowsUncheckedIOExceptionIfPathExistsButIsNoFolder() {
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isDirectory(path)).thenReturn(false);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s is a file", path));

			inTest.lastModified();
		}

	}

	public class ExistTests {

		@Test
		public void testExistsIfExisting() {
			when(nioAccess.isDirectory(path)).thenReturn(true);

			assertThat(inTest.exists(), is(true));
		}

		@Test
		public void testExistsIfNotExisting() {
			when(nioAccess.isDirectory(path)).thenReturn(false);

			assertThat(inTest.exists(), is(false));
		}

	}

	public class MoveToTests {

		@Test
		public void testMoveToThrowsIllegalArgumentExceptionIfTargetDoesNotBelongToSameFilesystem() {
			Folder target = mock(Folder.class);
			when(target.fileSystem()).thenReturn(mock(FileSystem.class));

			thrown.expect(IllegalArgumentException.class);
			thrown.expectMessage("Can only move a Folder to a Folder in the same FileSystem");

			inTest.moveTo(target);
		}

		@Test
		public void testMoveToWrapsIOExceptionThrownByNioAccessMoveInUncheckedIOException() throws IOException {
			NioFolder target = mock(NioFolder.class);
			Path targetPath = mock(Path.class);
			when(target.fileSystem()).thenReturn(fileSystem);
			when(target.path()).thenReturn(targetPath);
			when(target.parent()).thenReturn(Optional.empty());
			IOException exceptionThrownByMoveTo = new IOException();
			doThrow(exceptionThrownByMoveTo).when(nioAccess).move(path, targetPath);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionThrownByMoveTo));

			inTest.moveTo(target);
		}

		@Test
		public void testMoveToDeletesTargetAndDelegatesToNioAccessMoveIfTargetHasNoParent() throws IOException {
			NioFolder target = mock(NioFolder.class);
			Path targetPath = mock(Path.class);
			when(target.fileSystem()).thenReturn(fileSystem);
			when(target.path()).thenReturn(targetPath);
			when(target.parent()).thenReturn(Optional.empty());

			inTest.moveTo(target);

			InOrder inOrder = inOrder(target, nioAccess);
			inOrder.verify(target).delete();
			inOrder.verify(nioAccess).move(path, targetPath);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Test
		public void testMoveToDeletesTargetCreatesTargetsParentAndDelegatesToNioAccessMoveIfTargetHasAParent() throws IOException {
			NioFolder target = mock(NioFolder.class);
			NioFolder parentOfTarget = mock(NioFolder.class);
			Path targetPath = mock(Path.class);
			when(target.fileSystem()).thenReturn(fileSystem);
			when(target.path()).thenReturn(targetPath);
			when(target.parent()).thenReturn((Optional) Optional.of(parentOfTarget));

			inTest.moveTo(target);

			InOrder inOrder = inOrder(target, parentOfTarget, nioAccess);
			inOrder.verify(target).delete();
			inOrder.verify(parentOfTarget).create();
			inOrder.verify(nioAccess).move(path, targetPath);
		}

	}

	public class CreationTimeTests {

		@Test
		public void testCreationTimeDelegatesToNioAccessCreationTimeForExistingFolder() throws IOException {
			Instant exectedResult = Instant.parse("2016-01-08T19:49:00Z");
			when(nioAccess.getCreationTime(path)).thenReturn(FileTime.from(exectedResult));
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isDirectory(path)).thenReturn(true);

			Instant result = inTest.creationTime().get();

			assertThat(result, is(exectedResult));
		}

		@Test
		public void testCreationTimeDelegatesToNioAccessCreationTimeForNonExistingFolder() throws IOException {
			Instant exectedResult = Instant.parse("2016-01-08T19:49:00Z");
			when(nioAccess.getCreationTime(path)).thenReturn(FileTime.from(exectedResult));
			when(nioAccess.exists(path)).thenReturn(false);

			Instant result = inTest.creationTime().get();

			assertThat(result, is(exectedResult));
		}

		@Test
		public void testCreationTimeWrapsIOExceptionFromNioAccessCreationTimeInUncheckedIOException() throws IOException {
			IOException exceptionFromCreationTime = new IOException();
			when(nioAccess.getCreationTime(path)).thenThrow(exceptionFromCreationTime);
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isDirectory(path)).thenReturn(true);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromCreationTime));

			inTest.creationTime();
		}

		@Test
		public void testCreationTimeThrowsExceptionIfFileIsNoDirectory() {
			when(nioAccess.exists(path)).thenReturn(true);
			when(nioAccess.isDirectory(path)).thenReturn(false);

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(format("%s is a file", path));

			inTest.creationTime();
		}

	}

	public class DeleteTests {

		@Test
		public void testDeleteDoesNothingIfFolderDoesNotExist() {
			when(nioAccess.isDirectory(path)).thenReturn(false);

			inTest.delete();
		}

		@Test
		public void testDeleteInvokesDeleteOnChildFolderAndNioAccessDeleteAfterwards() throws IOException {
			Path folderChildPath = mock(Path.class);
			NioFolder folderChild = mock(NioFolder.class);
			when(nioAccess.isDirectory(path)).thenReturn(true);
			when(nioAccess.isDirectory(folderChildPath)).thenReturn(true);
			when(instanceFactory.nioFolder(Optional.of(inTest), folderChildPath, nioAccess)).thenReturn(folderChild);
			when(nioAccess.list(path)).thenAnswer(answerFrom(() -> Stream.of(folderChildPath)));

			inTest.delete();

			InOrder inOrder = inOrder(nioAccess, folderChild);
			inOrder.verify(nioAccess, times(2)).isDirectory(path);
			inOrder.verify(nioAccess).list(path);
			inOrder.verify(folderChild).delete();
			inOrder.verify(nioAccess).list(path);
			inOrder.verify(nioAccess).delete(path);
		}

		@Test
		public void testDeleteInvokesDeleteOnChildFileAndNioAccessDeleteAfterwards() throws IOException {
			Path fileChildPath = mock(Path.class);
			NioFile fileChild = mock(NioFile.class);
			when(nioAccess.isDirectory(path)).thenReturn(true);
			when(nioAccess.isDirectory(fileChildPath)).thenReturn(false);
			when(instanceFactory.nioFile(Optional.of(inTest), fileChildPath, nioAccess)).thenReturn(fileChild);
			when(nioAccess.list(path)).thenAnswer(answerFrom(() -> Stream.of(fileChildPath)));

			inTest.delete();

			InOrder inOrder = inOrder(nioAccess, fileChild);
			inOrder.verify(nioAccess, times(2)).isDirectory(path);
			inOrder.verify(nioAccess).list(path);
			inOrder.verify(nioAccess).list(path);
			inOrder.verify(fileChild).delete();
			inOrder.verify(nioAccess).delete(path);
		}

		@Test
		public void testDeleteWrapsIOExceptionFromFolderDeleteInUncheckedIOException() throws IOException {
			Path fileChildPath = mock(Path.class);
			NioFile fileChild = mock(NioFile.class);
			WritableFile writableFile = mock(WritableFile.class);
			when(fileChild.openWritable()).thenReturn(writableFile);
			when(nioAccess.isDirectory(path)).thenReturn(true);
			when(nioAccess.isDirectory(fileChildPath)).thenReturn(false);
			when(instanceFactory.nioFile(Optional.of(inTest), fileChildPath, nioAccess)).thenReturn(fileChild);
			when(nioAccess.list(path)).thenAnswer(answerFrom(() -> Stream.of(fileChildPath)));
			IOException exceptionFromDelete = new IOException();
			doThrow(exceptionFromDelete).when(nioAccess).delete(path);

			thrown.expect(UncheckedIOException.class);
			thrown.expectCause(is(exceptionFromDelete));

			inTest.delete();
		}

	}

	@Test
	public void testToString() {
		assertThat(inTest.toString(), is(format("NioFolder(%s)", path)));
	}

	private <T> Answer<T> answerFrom(Supplier<T> supplier) {
		return new Answer<T>() {
			@Override
			public T answer(InvocationOnMock invocation) throws Throwable {
				return supplier.get();
			}
		};
	}

}