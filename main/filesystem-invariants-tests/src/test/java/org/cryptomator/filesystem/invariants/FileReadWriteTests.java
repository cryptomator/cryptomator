package org.cryptomator.filesystem.invariants;

import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.hasContent;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.nio.ByteBuffer;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
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
public class FileReadWriteTests {

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<WayToObtainAFolder> WAYS_TO_OBTAIN_A_FOLDER = new WaysToObtainAFolder();

	@DataPoints
	public static final Iterable<WayToObtainAFile> WAYS_TO_OBTAIN_A_FILE = new WaysToObtainAFile();

	private static final String FILE_NAME = "fileName";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testWriteToNonExistingFileCreatesFileWithContent(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainANonExistingFile) {
		assumeThat(wayToObtainANonExistingFile.returnedFilesExist(), is(false));
		FileSystem fileSystem = fileSystemFactory.create();
		File file = wayToObtainANonExistingFile.fileWithName(fileSystem, FILE_NAME);
		byte[] dataToWrite = new byte[] {42, -43, 111, 104, -3, 83, -99, 30};

		try (WritableFile writable = file.openWritable()) {
			writable.write(ByteBuffer.wrap(dataToWrite));
		}

		assertThat(file, hasContent(dataToWrite));
	}

	@Theory
	public void testWriteToExistingFileOverwritesContent(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));
		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {32, 44, 1, -3, 4, 66, 4};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);
		byte[] dataToWrite = new byte[] {42, -43, 111, 104, -3, 83, -99, 30};

		try (WritableFile writable = file.openWritable()) {
			writable.write(ByteBuffer.wrap(dataToWrite));
		}

		assertThat(file, hasContent(dataToWrite));
	}

	@Theory
	public void testPartialWriteAtStartOfExistingFileOverwritesOnlyPartOfContents(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		// TODO implement partial writes in CryptoFileSystem
		assumeThat(fileSystemFactory.toString(), not(containsString("Crypto")));

		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {32, 44, 1, -3, 4, 66, 4};
		byte[] dataToWrite = new byte[] {1, 2, 3, 4};
		byte[] expectedData = new byte[] {1, 2, 3, 4, 4, 66, 4};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);

		try (WritableFile writable = file.openWritable()) {
			writable.write(ByteBuffer.wrap(dataToWrite));
		}

		assertThat(file, hasContent(expectedData));
	}

	@Theory
	public void testPartialWriteInTheMiddleOfExistingFileOverwritesOnlyPartOfContents(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		// TODO implement partial writes in CryptoFileSystem
		assumeThat(fileSystemFactory.toString(), not(containsString("Crypto")));

		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {32, 44, 1, -3, 4, 66, 4};
		byte[] dataToWrite = new byte[] {3, 4, 5, 6};
		byte[] expectedData = new byte[] {32, 44, 3, 4, 5, 6, 4};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);

		try (WritableFile writable = file.openWritable()) {
			writable.position(2);
			writable.write(ByteBuffer.wrap(dataToWrite));
		}

		assertThat(file, hasContent(expectedData));
	}

	@Theory
	public void testPartialWriteAtEndOfExistingFileOverwritesOnlyPartOfContents(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		// TODO implement partial writes in CryptoFileSystem
		assumeThat(fileSystemFactory.toString(), not(containsString("Crypto")));

		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {-1, 44, 1, -3, 4, 66, 4};
		byte[] dataToWrite = new byte[] {4, 5, 6, 7};
		byte[] expectedData = new byte[] {-1, 44, 1, 4, 5, 6, 7};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);

		try (WritableFile writable = file.openWritable()) {
			writable.position(3);
			writable.write(ByteBuffer.wrap(dataToWrite));
		}

		assertThat(file, hasContent(expectedData));
	}

}
