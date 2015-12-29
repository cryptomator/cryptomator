package org.cryptomator.filesystem.nio;

import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.containsInAnyOrder;
import static org.cryptomator.common.test.matcher.OptionalMatcher.presentOptionalWithValueThat;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.folder;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.NioNodeMatcher.fileWithName;
import static org.cryptomator.filesystem.nio.NioNodeMatcher.folderWithName;
import static org.cryptomator.filesystem.nio.PathMatcher.doesNotExist;
import static org.cryptomator.filesystem.nio.PathMatcher.isDirectory;
import static org.cryptomator.filesystem.nio.PathMatcher.isFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFolderTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNameIsNameOfFolder() throws IOException {
		final String folderName = "nameOfFolder";
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		Folder folder = fileSystem.folder(folderName);

		assertThat(folder, folderWithName(folderName));
	}

	@Test
	public void testCreateSucceedsIfFolderExists() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = testFilesystem( //
				folder(folderName));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder existingFolder = fileSystem.folder(folderName);

		existingFolder.create();

		assertThat(Files.isDirectory(testFilesystemPath.resolve(folderName)), is(true));
	}

	@Test
	public void testCreateSucceedsIfFolderDoesNotExist() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder nonExistingFolder = fileSystem.folder(folderName);

		nonExistingFolder.create();

		assertThat(Files.isDirectory(testFilesystemPath.resolve(folderName)), is(true));
	}

	@Test
	public void testCreateSucceedsIfParentIsMissing() throws IOException {
		String parentFolderName = "nameOfParentFolder";
		String folderName = "nameOfFolder";
		Path emptyFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystemPath);
		Folder folderWithNonExistingParent = fileSystem.folder(parentFolderName).folder(folderName);

		folderWithNonExistingParent.create();

		assertThat(Files.isDirectory(emptyFilesystemPath.resolve(parentFolderName).resolve(folderName)), is(true));
	}

	@Test
	public void testCreateWithFolderWhichIsAFileThrowsUncheckedIOExceptionWithAbsolutePathOfFolderInMessage() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = testFilesystem(file(folderName));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder folderWhichIsAFile = fileSystem.folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(testFilesystemPath.resolve(folderName).toString());

		folderWhichIsAFile.create();
	}

	@Test
	public void testChildrenOfEmptyNioFolderAreEmpty() throws IOException {
		NioFolder folder = NioFileSystem.rootedAt(emptyFilesystem());

		assertThat(folder.children().collect(toList()), is(empty()));
	}

	@Test
	public void testChildrenOfNonExistingFolderThrowsUncheckedIOExceptionWithAbolutePathOfFolderInMessage() throws IOException {
		String nameOfNonExistingFolder = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(nameOfNonExistingFolder);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(nameOfNonExistingFolder).toString());

		folder.children();
	}

	@Test
	public void testChildrenOfFolderWhichIsAFileThrowsUncheckedIOExceptionWithAbsolutePathOfFolderInMessage() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem(file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(folderName).toString());

		folder.children();
	}

	@Test
	public void testChildrenOfFolderAreCorrect() throws IOException {
		String folderName1 = "folder1";
		String folderName2 = "folder2";
		String fileName1 = "file1";
		String fileName2 = "file2";

		NioFolder folder = NioFileSystem.rootedAt(testFilesystem( //
				folder(folderName1), //
				folder(folderName2), //
				file(fileName1), //
				file(fileName2)));

		assertThat(folder.children().collect(toList()),
				containsInAnyOrder( //
						folderWithName(folderName1), //
						folderWithName(folderName2), //
						fileWithName(fileName1), //
						fileWithName(fileName2)));
	}

	@Test
	public void testFilesDoesContainOnlyFileChildren() throws IOException {
		String folderName1 = "folder1";
		String folderName2 = "folder2";
		String fileName1 = "file1";
		String fileName2 = "file2";

		NioFolder folder = NioFileSystem.rootedAt(testFilesystem( //
				folder(folderName1), //
				folder(folderName2), //
				file(fileName1), //
				file(fileName2)));

		assertThat(folder.files().collect(toList()),
				containsInAnyOrder( //
						fileWithName(fileName1), //
						fileWithName(fileName2)));
	}

	@Test
	public void testFilesOfNonExistingFolderThrowsUncheckedIOExceptionWithAbolutePathOfFolderInMessage() throws IOException {
		Path emptyFolderPath = emptyFilesystem();
		NioFolder folder = NioFileSystem.rootedAt(emptyFolderPath);
		Files.delete(emptyFolderPath);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(emptyFolderPath.toString());

		folder.files();
	}

	@Test
	public void testFoldersDoesContainOnlyFolderChildren() throws IOException {
		String folderName1 = "folder1";
		String folderName2 = "folder2";
		String fileName1 = "file1";
		String fileName2 = "file2";

		NioFolder folder = NioFileSystem.rootedAt(testFilesystem( //
				folder(folderName1), //
				folder(folderName2), //
				file(fileName1), //
				file(fileName2)));

		assertThat(folder.folders().collect(toList()),
				containsInAnyOrder( //
						folderWithName(folderName1), //
						folderWithName(folderName2)));
	}

	@Test
	public void testFoldersOfNonExistingFolderThrowsUncheckedIOExceptionWithAbolutePathOfFolderInMessage() throws IOException {
		Path emptyFilesystemPath = emptyFilesystem();
		NioFolder folder = NioFileSystem.rootedAt(emptyFilesystemPath);
		Files.delete(emptyFilesystemPath);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(emptyFilesystemPath.toString());

		folder.folders();
	}

	@Test
	public void testDeleteOfEmptyFolderDeletesIt() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				folder(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		folder.delete();

		assertThat(filesystemPath.resolve(folderName), doesNotExist());
	}

	@Test
	public void testDeleteOfFolderWithChildrenDeletesItAndAllChildrenRecursive() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				folder(folderName), //
				folder(folderName + "/subfolder1"), //
				file(folderName + "/subfolder1/fileName1"), //
				folder(folderName + "/subfolder2"), //
				file(folderName + "/fileName1"), //
				file(folderName + "/fileName2"));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		folder.delete();

		assertThat(filesystemPath.resolve(folderName), doesNotExist());
	}

	@Test
	public void testDeleteOfNonExistingFolderDoesNothing() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder nonExistingFolder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		nonExistingFolder.delete();

		assertThat(filesystemPath.resolve(folderName), doesNotExist());
	}

	@Test
	public void testDeleteOfFolderWhichIsAFileDoesNothing() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		folder.delete();

		assertThat(filesystemPath.resolve(folderName), isFile());
	}

	@Test
	public void testExistsReturnsTrueForExistingDirectory() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				folder(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(folder.exists(), is(true));
	}

	@Test
	public void testExistsReturnsFalseForNonExistingDirectory() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder nonExistingFolder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(nonExistingFolder.exists(), is(false));
	}

	@Test
	public void testExistsReturnsFalseForDirectoryWhichIsAFile() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(folder.exists(), is(false));
	}

	@Test
	public void testIsAncestorOfWithChildReturnsTrue() throws IOException {
		Folder folder = NioFileSystem.rootedAt(emptyFilesystem()).folder("a");
		Folder child = folder.folder("b");

		assertThat(folder.isAncestorOf(child), is(true));
	}

	@Test
	public void testIsAncestorOfWithChildOfChildReturnsTrue() throws IOException {
		Folder folder = NioFileSystem.rootedAt(emptyFilesystem()).folder("a");
		Folder child = folder.folder("b");
		File childOfChild = child.file("c");

		assertThat(folder.isAncestorOf(childOfChild), is(true));
	}

	@Test
	public void testIsAncestorOfWithSiblingReturnsFalse() throws IOException {
		FileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		Folder folder = fileSystem.folder("a");
		Folder sibling = fileSystem.folder("b");

		assertThat(folder.isAncestorOf(sibling), is(false));
	}

	@Test
	public void testIsAncestorOfWithParentReturnsFalse() throws IOException {
		Folder parent = NioFileSystem.rootedAt(emptyFilesystem()).folder("a");
		Folder folder = parent.folder("b");

		assertThat(folder.isAncestorOf(parent), is(false));
	}

	@Test
	public void testLastModifiedOfExistingFolderReturnsLastModifiedDate() throws IOException {
		String folderName = "nameOfFolder";
		Instant lastModified = Instant.parse("2015-12-29T15:36:10.00Z");
		Folder folder = NioFileSystem
				.rootedAt(testFilesystem( //
						folder(folderName).withLastModified(lastModified))) //
				.folder(folderName);

		assertThat(folder.lastModified(), is(lastModified));
	}

	@Test
	public void testLastModifiedOfNonExistingFolderThrowsUncheckedIOExceptionWithAbsolutePathOfFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(folderName).toString());

		folder.lastModified();
	}

	@Test
	public void testLastModifiedOfFolderWhichIsAFileThrowsUncheckedIOExceptionWithAbsolutePathOfFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem(file(folderName));
		Folder folder = NioFileSystem //
				.rootedAt(filesystemPath) //
				.folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(folderName).toString());

		folder.lastModified();
	}

	@Test
	public void testParentOfDirectChildOfFilesystemReturnsFilesystem() throws IOException {
		FileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		Folder folder = fileSystem.folder("aName");

		assertThat(folder.parent(), presentOptionalWithValueThat(is(sameInstance(fileSystem))));
	}

	@Test
	public void testParentOfChildOfFolderReturnsFolder() throws IOException {
		Folder folder = NioFileSystem.rootedAt(emptyFilesystem()).folder("aName");
		Folder child = folder.folder("anotherName");

		assertThat(child.parent(), presentOptionalWithValueThat(is(sameInstance(folder))));
	}

	@Test
	public void testMoveToOfFolderToNonExistingFolderMovesFolder() throws IOException {
		Path filesystemPath = testFilesystem( //
				folder("folderToMove"));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Folder folderToMove = fileSystem.folder("folderToMove");
		Folder folderToMoveTo = fileSystem.folder("folderToMoveTo");

		folderToMove.moveTo(folderToMoveTo);

		assertThat(filesystemPath.resolve("folderToMove"), doesNotExist());
		assertThat(filesystemPath.resolve("folderToMoveTo"), isDirectory());
	}

	@Test
	public void testMoveToOfFolderWithChildrenMovesFolderAndChildren() throws IOException {
		Path filesystemPath = testFilesystem( //
				folder("folderToMove"), //
				folder("folderToMove/subfolder1"), //
				folder("folderToMove/subfolder2"), //
				file("folderToMove/subfolder1/file1"), //
				file("folderToMove/file2"), //
				file("folderToMove/file3"));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Folder folderToMove = fileSystem.folder("folderToMove");
		Folder folderToMoveTo = fileSystem.folder("folderToMoveTo");

		folderToMove.moveTo(folderToMoveTo);

		assertThat(filesystemPath.resolve("folderToMove"), doesNotExist());
		assertThat(filesystemPath.resolve("folderToMoveTo"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder1"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder2"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder1/file1"), isFile());
		assertThat(filesystemPath.resolve("folderToMoveTo/file2"), isFile());
		assertThat(filesystemPath.resolve("folderToMoveTo/file3"), isFile());
	}

	@Test
	public void testMoveToOfFolderToExistingFolderReplacesTargetFolder() throws IOException {
		// TODO Markus Kreusch implement test
	}

	@Test
	public void testMoveToOfFolderToExistingFileThrowsUncheckedIOExceptionWithAbsolutePathOfTarget() throws IOException {
		// TODO Markus Kreusch implement test
	}

}
