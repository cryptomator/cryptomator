package org.cryptomator.filesystem.invariants;

import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.hasContent;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
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

	@Theory
	public void testConcurrentPartialReadsDontInterfere(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) throws InterruptedException, ExecutionException {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {32, 44, 1, -3, 4, 66, 4};
		byte[] expectedData1 = new byte[] {-3, 4, 66};
		byte[] expectedData2 = new byte[] {44, 1, -3, 4};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);

		// control flag to make sure thread timing is synchronized correctly
		AtomicInteger state = new AtomicInteger();

		// set position, then wait before read:
		byte[] actualData1 = new byte[3];
		Callable<?> readTask1 = () -> {
			try (ReadableFile readable = file.openReadable()) {
				ByteBuffer buf = ByteBuffer.wrap(actualData1);
				readable.position(3);
				assertTrue("readTask1 must be the first to set its position", state.compareAndSet(0, 1));
				Thread.sleep(20);
				assertTrue("readTask1 must be the last to actually read data", state.compareAndSet(3, 4));
				readable.read(buf);
				return null;
			}
		};

		// wait, then set position and read:
		byte[] actualData2 = new byte[4];
		Callable<?> readTask2 = () -> {
			try (ReadableFile readable = file.openReadable()) {
				ByteBuffer buf = ByteBuffer.wrap(actualData2);
				Thread.sleep(10);
				readable.position(1);
				assertTrue("readTask2 must be second to set its position", state.compareAndSet(1, 2));
				readable.read(buf);
				assertTrue("readTask2 must be first to finish reading", state.compareAndSet(2, 3));
				return null;
			}
		};

		// start both read tasks at the same time:
		ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		executor.prestartAllCoreThreads();
		Future<?> task1Completed = executor.submit(readTask1);
		Future<?> task2Completed = executor.submit(readTask2);
		task1Completed.get();
		task2Completed.get();
		executor.shutdown();

		assertArrayEquals(expectedData1, actualData1);
		assertArrayEquals(expectedData2, actualData2);
	}

}
