package org.cryptomator.filesystem.invariants;

import static org.cryptomator.common.test.matcher.OptionalMatcher.presentOptionalWithValueThat;
import static org.cryptomator.filesystem.invariants.matchers.InstantMatcher.inRangeInclusiveWithTolerance;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.invariants.WaysToObtainAFile.WayToObtainAFile;
import org.cryptomator.filesystem.invariants.WaysToObtainAFolder.WayToObtainAFolder;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FileTests {

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<WayToObtainAFolder> WAYS_TO_OBTAIN_A_FOLDER = new WaysToObtainAFolder();

	@DataPoints
	public static final Iterable<WayToObtainAFile> WAYS_TO_OBTAIN_A_FILE = new WaysToObtainAFile();

	private static final String FILE_NAME = "fileName";

	private static final String FOLDER_NAME = "folderName";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testNonExistingFileDoesNotExist(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.exists(), is(false));
	}

	@Theory
	public void testExistingFileExist(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAnExistingFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.exists(), is(true));
	}

	@Theory
	public void testNameOfFileIsFileName(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.name(), is(FILE_NAME));
	}

	@Theory
	public void testDeletedFileDoesNotExist(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);
		file.delete();

		assertThat(file.exists(), is(false));
	}

	@Theory
	public void testParentOfFileInFilesystemIsFilesystem(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.parent(), is(presentOptionalWithValueThat(is(fileSystem))));
	}

	@Theory
	public void testParentOfFileInFolderIsFolder(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAFolder, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		Folder folder = wayToObtainAFolder.folderWithName(fileSystem, FOLDER_NAME);
		File file = wayToObtainAFile.fileWithName(folder, FILE_NAME);

		assertThat(file.parent(), is(presentOptionalWithValueThat(is(folder))));
	}

	@Theory
	public void testFilesystemOfFileInFilesystemInFilesystem(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.fileSystem(), is(fileSystem));
	}

	@Theory
	public void testFilesystemOfFileInFolderIsFilesystem(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAFolder, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		Folder folder = wayToObtainAFolder.folderWithName(fileSystem, FOLDER_NAME);
		File file = wayToObtainAFile.fileWithName(folder, FILE_NAME);

		assertThat(file.fileSystem(), is(fileSystem));
	}

	@Theory
	public void testFilesFromTwoFileSystemsDoNotBelongToSameFilesystem(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		File file = wayToObtainAFile.fileWithName(fileSystemFactory.create(), FILE_NAME);
		File otherFile = wayToObtainAFile.fileWithName(fileSystemFactory.create(), FILE_NAME);

		assertThat(file.belongsToSameFilesystem(otherFile), is(false));
	}

	@Theory
	public void testFilesFromSameFileSystemsDoBelongToSameFilesystem(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);
		File otherFile = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.belongsToSameFilesystem(otherFile), is(true));
	}

	@Theory
	public void testFilesBelongToSameFilesystemAsItself(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAFile) {
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainAFile.fileWithName(fileSystem, FILE_NAME);

		assertThat(file.belongsToSameFilesystem(file), is(true));
	}

	@Theory
	public void testLastModifiedIsInCorrectSecondsRange(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Instant min = Instant.now();
		File file = wayToObtainAnExistingFile.fileWithName(fileSystem, FILE_NAME);
		Instant max = Instant.now();

		assertThat(file.lastModified(), is(inRangeInclusiveWithTolerance(min, max, 2000)));
	}

	@Theory
	public void testLastModifiedThrowsUncheckedIoExceptionForNonExistingFile(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();

		thrown.expect(UncheckedIOException.class);

		System.out.println(wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME).lastModified());
	}

	@Theory
	public void testCanNotOpenFileWhichExistsAsFolderForReading(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile, WayToObtainAFolder wayToObtainAnExistingFolder) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));
		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME);
		wayToObtainAnExistingFolder.folderWithName(fileSystem, FILE_NAME);

		thrown.expect(UncheckedIOException.class);

		file.openReadable();
	}

	@Theory
	public void testCanNotCreateFileWhichExistsAsFolderByWriting(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile, WayToObtainAFolder wayToObtainAnExistingFolder) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));
		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME);
		wayToObtainAnExistingFolder.folderWithName(fileSystem, FILE_NAME);

		thrown.expect(UncheckedIOException.class);

		try (WritableFile writable = file.openWritable()) {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			writable.write(buffer);
		}
	}

	@Theory
	public void testCanNotReadFromNonExistingFile(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME);

		thrown.expect(UncheckedIOException.class);

		file.openReadable();
	}

}
