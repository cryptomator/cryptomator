package org.cryptomator.filesystem.nio.integrationtests;

import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.containsInAnyOrder;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.folder;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.NioNodeMatcher.fileWithName;
import static org.cryptomator.filesystem.nio.integrationtests.NioNodeMatcher.folderWithName;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.doesNotExist;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.isDirectory;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.isFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFolderIntegrationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testCreateExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = testFilesystem( //
				folder(folderName));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder existingFolder = fileSystem.folder(folderName);

		existingFolder.create();

		assertThat(Files.isDirectory(testFilesystemPath.resolve(folderName)), is(true));
	}

	@Test
	public void testCreateNonExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder nonExistingFolder = fileSystem.folder(folderName);

		nonExistingFolder.create();

		assertThat(Files.isDirectory(testFilesystemPath.resolve(folderName)), is(true));
	}

	@Test
	public void testCreateFolderWithMissingParent() throws IOException {
		String parentFolderName = "nameOfParentFolder";
		String folderName = "nameOfFolder";
		Path emptyFilesystemPath = emptyFilesystem();
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystemPath);
		Folder folderWithNonExistingParent = fileSystem.folder(parentFolderName).folder(folderName);

		folderWithNonExistingParent.create();

		assertThat(Files.isDirectory(emptyFilesystemPath.resolve(parentFolderName).resolve(folderName)), is(true));
	}

	@Test
	public void testCreateFolderWhichIsAFile() throws IOException {
		String folderName = "nameOfFolder";
		Path testFilesystemPath = testFilesystem(file(folderName));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(testFilesystemPath);
		Folder folderWhichIsAFile = fileSystem.folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(testFilesystemPath.resolve(folderName).toString());

		folderWhichIsAFile.create();
	}

	@Test
	public void testChildrenOfEmptyFolder() throws IOException {
		Folder folder = NioFileSystem.rootedAt(emptyFilesystem());

		assertThat(folder.children().collect(toList()), is(empty()));
	}

	@Test
	public void testChildrenOfNonExistingFolder() throws IOException {
		String nameOfNonExistingFolder = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(nameOfNonExistingFolder);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(nameOfNonExistingFolder).toString());

		folder.children();
	}

	@Test
	public void testChildrenOfFolderWhichIsAFile() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem(file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(folderName).toString());

		folder.children();
	}

	@Test
	public void testChildrenOfFolder() throws IOException {
		String folderName1 = "folder1";
		String folderName2 = "folder2";
		String fileName1 = "file1";
		String fileName2 = "file2";

		Folder folder = NioFileSystem.rootedAt(testFilesystem( //
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
	public void testDeleteOfEmptyFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				folder(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		folder.delete();

		assertThat(filesystemPath.resolve(folderName), doesNotExist());
	}

	@Test
	public void testDeleteOfFolderWithChildren() throws IOException {
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
	public void testDeleteOfNonExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder nonExistingFolder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		nonExistingFolder.delete();

		assertThat(filesystemPath.resolve(folderName), doesNotExist());
	}

	@Test
	public void testDeleteOfFolderWhichIsAFile() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		folder.delete();

		assertThat(filesystemPath.resolve(folderName), isFile());
	}

	@Test
	public void testExistsForExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				folder(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(folder.exists(), is(true));
	}

	@Test
	public void testExistsForNonExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder nonExistingFolder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(nonExistingFolder.exists(), is(false));
	}

	@Test
	public void testExistsForFolderWhichIsAFile() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = testFilesystem( //
				file(folderName));
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		assertThat(folder.exists(), is(false));
	}

	@Test
	public void testLastModifiedOfExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Instant lastModified = Instant.parse("2015-12-29T15:36:10.00Z");
		Folder folder = NioFileSystem
				.rootedAt(testFilesystem( //
						folder(folderName).withLastModified(lastModified))) //
				.folder(folderName);

		assertThat(folder.lastModified(), is(lastModified));
	}

	@Test
	public void testLastModifiedOfNonExistingFolder() throws IOException {
		String folderName = "nameOfFolder";
		Path filesystemPath = emptyFilesystem();
		Folder folder = NioFileSystem.rootedAt(filesystemPath).folder(folderName);

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve(folderName).toString());

		folder.lastModified();
	}

	@Test
	public void testLastModifiedOfFolderWhichIsAFile() throws IOException {
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
	public void testMoveToOfFolderToNonExistingFolder() throws IOException {
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
	public void testMoveToOfFolderWithChildren() throws IOException {
		Path filesystemPath = testFilesystem( //
				folder("folderToMove"), //
				folder("folderToMove/subfolder1"), //
				folder("folderToMove/subfolder2"), //
				file("folderToMove/subfolder1/file1").withData("dataOfFile1"), //
				file("folderToMove/file2").withData("dataOfFile2"), //
				file("folderToMove/file3").withData("dataOfFile3"));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Folder folderToMove = fileSystem.folder("folderToMove");
		Folder folderToMoveTo = fileSystem.folder("folderToMoveTo");

		folderToMove.moveTo(folderToMoveTo);

		assertThat(filesystemPath.resolve("folderToMove"), doesNotExist());
		assertThat(filesystemPath.resolve("folderToMoveTo"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder1"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder2"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/subfolder1/file1"), isFile().withContent("dataOfFile1"));
		assertThat(filesystemPath.resolve("folderToMoveTo/file2"), isFile().withContent("dataOfFile2"));
		assertThat(filesystemPath.resolve("folderToMoveTo/file3"), isFile().withContent("dataOfFile3"));
	}

	@Test
	public void testMoveToOfFolderToExistingFolder() throws IOException {
		Path filesystemPath = testFilesystem( //
				folder("folderToMove"), //
				file("folderToMove/file1").withData("dataOfFile1"), //
				file("folderToMove/file2").withData("dataOfFile2"), //
				folder("folderToMoveTo"), //
				file("folderToMoveTo/file1").withData("wrongDataOfFile1"), //
				file("folderToMoveTo/fileWhichShouldNotExistAfterMove"));
		NioFileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Folder folderToMove = fileSystem.folder("folderToMove");
		Folder folderToMoveTo = fileSystem.folder("folderToMoveTo");

		folderToMove.moveTo(folderToMoveTo);

		assertThat(filesystemPath.resolve("folderToMove"), doesNotExist());
		assertThat(filesystemPath.resolve("folderToMoveTo"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo/file1"), isFile().withContent("dataOfFile1"));
		assertThat(filesystemPath.resolve("folderToMoveTo/file2"), isFile().withContent("dataOfFile2"));
		assertThat(filesystemPath.resolve("folderToMoveTo/fileWhichShouldNotExistAfterMove"), doesNotExist());
	}

	@Test
	public void testMoveToOfFolderToExistingFile() throws IOException {
		Path filesystemPath = testFilesystem( //
				folder("folderToMove"), //
				file("folderToMoveTo"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Folder folderToMove = fileSystem.folder("folderToMove");
		Folder folderToMoveTo = fileSystem.folder("folderToMoveTo");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filesystemPath.resolve("folderToMoveTo").toString());

		folderToMove.moveTo(folderToMoveTo);
	}

	@Test
	public void testMoveToOfFolderToFolderOfAnotherFileSystem() throws IOException {
		Path filesystemPath = testFilesystem(folder("folderToMove"));
		Folder folderToMove = NioFileSystem.rootedAt(filesystemPath).folder("folderToMove");
		Folder folderToMoveTo = NioFileSystem.rootedAt(filesystemPath).folder("folderToMoveTo");

		thrown.expect(IllegalArgumentException.class);

		folderToMove.moveTo(folderToMoveTo);

		assertThat(filesystemPath.resolve("folderToMove"), isDirectory());
		assertThat(filesystemPath.resolve("folderToMoveTo"), doesNotExist());
	}

}