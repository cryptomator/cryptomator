package org.cryptomator.filesystem.nio;

import static java.util.stream.Collectors.toList;
import static org.cryptomator.commons.test.matcher.ContainsMatcher.containsInAnyOrder;
import static org.cryptomator.filesystem.FolderCreateMode.FAIL_IF_PARENT_IS_MISSING;
import static org.cryptomator.filesystem.FolderCreateMode.INCLUDING_PARENTS;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.folder;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.NioNodeMatcher.fileWithName;
import static org.cryptomator.filesystem.nio.NioNodeMatcher.folderWithName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cryptomator.filesystem.Folder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFolderTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNameIsNameOfFolder() throws IOException {
		final String folderName = "folderNameABC";
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		Folder folder = fileSystem.folder(folderName);

		assertThat(folder, folderWithName(folderName));
	}

	@Test
	public void testCreateWithOptionFailIfParentIsMissingFailsIfParentIsMissing() throws IOException {
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		Folder folderWithNonExistingParent = fileSystem.folder("a").folder("b");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(fileSystem.path.resolve("a/b").toString());

		folderWithNonExistingParent.create(FAIL_IF_PARENT_IS_MISSING);
	}

	@Test
	public void testCreateWithOptionIncludingParentsSucceedsIfParentIsMissing() throws IOException {
		Path emptyFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystemPath);
		Folder folderWithNonExistingParent = fileSystem.folder("a").folder("b");

		folderWithNonExistingParent.create(INCLUDING_PARENTS);

		assertThat(Files.isDirectory(emptyFilesystemPath.resolve("a/b")), is(true));
	}

	@Test
	public void testCreateWithOptionFailIfParentIsMissingSucceedsIfParentIsPresent() throws IOException {
		Path emptyFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystemPath);
		Folder nonExistingFolder = fileSystem.folder("a");

		nonExistingFolder.create(FAIL_IF_PARENT_IS_MISSING);

		assertThat(Files.isDirectory(emptyFilesystemPath.resolve("a")), is(true));
	}

	@Test
	public void testChildrenOfEmptyNioFolderAreEmpty() throws IOException {
		NioFolder folder = NioFileSystem.rootedAt(emptyFilesystem());

		assertThat(folder.children().collect(toList()), is(empty()));
	}

	@Test
	public void testChildrenOfNonExistingFolderThrowsUncheckedIOExceptionWithAbolutePathOfFolderInMessage() throws IOException {
		Path emptyFolderPath = emptyFilesystem();
		NioFolder folder = NioFileSystem.rootedAt(emptyFolderPath);
		Files.delete(emptyFolderPath);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(emptyFolderPath.toString());

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
}
