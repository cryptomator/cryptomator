package org.cryptomator.filesystem.nio.integrationtests;

import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.folder;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.doesNotExist;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.isFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.nio.NioFileSystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFileIntegrationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testExistsForExistingFile() {
		File existingFile = NioFileSystem.rootedAt(testFilesystem(file("testFile"))) //
				.file("testFile");

		assertThat(existingFile.exists(), is(true));
	}

	@Test
	public void testExistsForNonExistingFile() {
		File nonExistingFile = NioFileSystem.rootedAt(emptyFilesystem()) //
				.file("testFile");

		assertThat(nonExistingFile.exists(), is(false));
	}

	@Test
	public void testExistsForFileWhichIsAFolder() {
		File fileWhichIsAFolder = NioFileSystem.rootedAt(testFilesystem(folder("nameOfAnExistingFolder"))) //
				.file("nameOfAnExistingFolder");

		assertThat(fileWhichIsAFolder.exists(), is(false));
	}

	@Test
	public void testLastModifiedForExistingFile() {
		Instant expectedLastModified = Instant.parse("2015-12-31T15:03:34Z");
		File existingFile = NioFileSystem
				.rootedAt(testFilesystem( //
						file("testFile").withLastModified(expectedLastModified))) //
				.file("testFile");

		assertThat(existingFile.lastModified(), is(expectedLastModified));
	}

	@Test
	public void testLastModifiedForNonExistingFile() {
		Path filesystemPath = emptyFilesystem();
		Path pathOfNonExistingFile = filesystemPath.resolve("nonExistingFile");
		File nonExistingFile = NioFileSystem.rootedAt(filesystemPath) //
				.file("nonExistingFile");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(pathOfNonExistingFile.toString());

		nonExistingFile.lastModified();
	}

	@Test
	public void testLastModifiedForFileWhichIsAFolder() {
		Path filesystemPath = testFilesystem(folder("nameOfAnExistingFolder"));
		Path pathOfNonExistingFile = filesystemPath.resolve("nameOfAnExistingFolder");
		File fileWhichIsAFolder = NioFileSystem.rootedAt(filesystemPath) //
				.file("nameOfAnExistingFolder");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(pathOfNonExistingFile.toString());

		fileWhichIsAFolder.lastModified();
	}

	@Test
	public void testCopyToNonExistingTarget() {
		Path filesystemPath = testFilesystem(file("sourceFile").withData("fileContents"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path sourceFilePath = filesystemPath.resolve("sourceFile");
		Path targetFilePath = filesystemPath.resolve("targetFile");
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("targetFile");

		source.copyTo(target);

		assertThat(sourceFilePath, isFile().withContent("fileContents"));
		assertThat(targetFilePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testCopyToExistingTarget() {
		Path filesystemPath = testFilesystem( //
				file("sourceFile").withData("fileContents"), //
				file("targetFile").withData("wrongFileContents"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path sourceFilePath = filesystemPath.resolve("sourceFile");
		Path targetFilePath = filesystemPath.resolve("targetFile");
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("targetFile");

		source.copyTo(target);

		assertThat(sourceFilePath, isFile().withContent("fileContents"));
		assertThat(targetFilePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testCopyToFolderTarget() {
		Path filesystemPath = testFilesystem( //
				file("sourceFile").withData("fileContents"), //
				folder("aFolderName"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path targetFilePath = filesystemPath.resolve("aFolderName").toAbsolutePath();
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("aFolderName");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(targetFilePath.toAbsolutePath().toString());

		source.copyTo(target);
	}

	@Test
	public void testCopyToOfNonExistingFile() {
		Path filesystemPath = emptyFilesystem();
		Path filePath = filesystemPath.resolve("nonExistingFile").toAbsolutePath();
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		File nonExistingFile = fileSystem.file("nonExistingFile");
		File target = fileSystem.file("target");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filePath.toString());

		nonExistingFile.copyTo(target);
	}

	@Test
	public void testCopyToOfFileWhichIsAFolder() {
		Path filesystemPath = testFilesystem(folder("folderName"));
		Path filePath = filesystemPath.resolve("folderName").toAbsolutePath();
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		File fileWhichIsAFolder = fileSystem.file("folderName");
		File target = fileSystem.file("target");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filePath.toString());

		fileWhichIsAFolder.copyTo(target);
	}

	@Test
	public void testMoveToNonExistingTarget() {
		Path filesystemPath = testFilesystem(file("sourceFile").withData("fileContents"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path sourceFilePath = filesystemPath.resolve("sourceFile");
		Path targetFilePath = filesystemPath.resolve("targetFile");
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("targetFile");

		source.moveTo(target);

		assertThat(sourceFilePath, doesNotExist());
		assertThat(targetFilePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testMoveToExistingTarget() {
		Path filesystemPath = testFilesystem( //
				file("sourceFile").withData("fileContents"), //
				file("targetFile").withData("wrongFileContents"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path sourceFilePath = filesystemPath.resolve("sourceFile");
		Path targetFilePath = filesystemPath.resolve("targetFile");
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("targetFile");

		source.moveTo(target);

		assertThat(sourceFilePath, doesNotExist());
		assertThat(targetFilePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testMoveToSameFile() {
		Path filesystemPath = testFilesystem(file("fileName").withData("fileContents"));
		Path filePath = filesystemPath.resolve("fileName");
		File file = NioFileSystem.rootedAt(filesystemPath).file("fileName");

		file.moveTo(file);

		assertThat(filePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testMoveToDirectoryTarget() {
		Path filesystemPath = testFilesystem( //
				file("sourceFile").withData("fileContents"), //
				folder("aFolderName"));
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		Path targetFilePath = filesystemPath.resolve("aFolderName").toAbsolutePath();
		File source = fileSystem.file("sourceFile");
		File target = fileSystem.file("aFolderName");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(targetFilePath.toAbsolutePath().toString());

		source.moveTo(target);
	}

	@Test
	public void testMoveToOfNonExistingFile() {
		Path filesystemPath = emptyFilesystem();
		Path filePath = filesystemPath.resolve("nonExistingFile").toAbsolutePath();
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		File nonExistingFile = fileSystem.file("nonExistingFile");
		File target = fileSystem.file("target");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filePath.toString());

		nonExistingFile.moveTo(target);
	}

	@Test
	public void testMoveToOfFileWhichIsAFolder() {
		Path filesystemPath = testFilesystem(folder("folderName"));
		Path filePath = filesystemPath.resolve("folderName").toAbsolutePath();
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		File fileWhichIsAFolder = fileSystem.file("folderName");
		File target = fileSystem.file("target");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filePath.toString());

		fileWhichIsAFolder.moveTo(target);
	}

	@Test
	public void testOpenWritableDoesNotCreateANonExisitingFile() {
		Path filesystemPath = emptyFilesystem();
		Path filePath = filesystemPath.resolve("nonExistingFile");
		File file = NioFileSystem.rootedAt(filesystemPath).file("nonExistingFile");

		file.openWritable();

		assertThat(filePath, doesNotExist());
	}

}