package org.cryptomator.filesystem.invariants;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assume.assumeThat;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;

import org.cryptomator.common.RunnableThrowingException;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.ReadableFile;
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
public class ConcurrencyTests {

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
	public void testConcurrentPartialReadsDontInterfere(FileSystemFactory fileSystemFactory, WayToObtainAFile wayToObtainAnExistingFile) throws ExecutionException {
		assumeThat(wayToObtainAnExistingFile.returnedFilesExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		byte[] originalData = new byte[] {32, 44, 1, -3, 4, 66, 4};
		byte[] expectedData1 = new byte[] {-3, 4, 66};
		byte[] expectedData2 = new byte[] {44, 1, -3, 4};
		File file = wayToObtainAnExistingFile.fileWithNameAndContent(fileSystem, FILE_NAME, originalData);

		TasksInThreadRunner thread1 = new TasksInThreadRunner();
		TasksInThreadRunner thread2 = new TasksInThreadRunner();

		Holder<ReadableFile> readableFile1 = new Holder<>();
		Holder<ReadableFile> readableFile2 = new Holder<>();
		byte[] actualData1 = new byte[3];
		byte[] actualData2 = new byte[4];

		thread1.runAndWaitFor(() -> readableFile1.value = file.openReadable());
		thread2.runAndWaitFor(() -> readableFile2.value = file.openReadable());
		thread1.runAndWaitFor(() -> readableFile1.value.position(3));
		thread2.runAndWaitFor(() -> {
			readableFile2.value.position(1);
			readableFile2.value.read(ByteBuffer.wrap(actualData2));
		});
		thread1.runAndWaitFor(() -> readableFile1.value.read(ByteBuffer.wrap(actualData1)));
		thread1.runAndWaitFor(readableFile1.value::close);
		thread2.runAndWaitFor(readableFile2.value::close);

		thread1.shutdown();
		thread2.shutdown();

		assertArrayEquals(expectedData1, actualData1);
		assertArrayEquals(expectedData2, actualData2);
	}

	private static class Holder<T> {

		T value;

	}

	private static class TasksInThreadRunner {

		private final Runnable TERMINATION_HINT = () -> {
		};

		private final SynchronousQueue<Runnable> handoverQueue = new SynchronousQueue<>();
		private final Thread thread = new Thread(() -> {
			try {
				Runnable task;
				while ((task = handoverQueue.take()) != TERMINATION_HINT) {
					task.run();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		});

		public TasksInThreadRunner() {
			thread.start();
		}

		public void runAndWaitFor(RunnableThrowingException<?> task) throws ExecutionException {
			CompletableFuture<Void> future = new CompletableFuture<>();
			try {
				handoverQueue.put(() -> {
					try {
						task.run();
						future.complete(null);
					} catch (Throwable e) {
						future.completeExceptionally(e);
					}
				});
				future.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		public void shutdown() {
			try {
				handoverQueue.put(TERMINATION_HINT);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

}
