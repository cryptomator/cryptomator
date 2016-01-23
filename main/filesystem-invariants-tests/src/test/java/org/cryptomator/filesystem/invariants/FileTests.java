package org.cryptomator.filesystem.invariants;

import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.hasContent;
import static org.hamcrest.CoreMatchers.is;
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
public class FileTests {

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

}
