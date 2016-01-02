package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.common.test.matcher.OptionalMatcher.presentOptionalWithValueThat;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.folder;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.PathMatcher.doesNotExist;
import static org.cryptomator.filesystem.nio.PathMatcher.isFile;
import static org.cryptomator.filesystem.nio.ThreadStackMatcher.stackContains;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class NioFileTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testExistsForExistingFileReturnsTrue() {
		File existingFile = NioFileSystem.rootedAt(testFilesystem(file("testFile"))) //
				.file("testFile");

		assertThat(existingFile.exists(), is(true));
	}

	@Test
	public void testExistsForNonExistingFileReturnsFalse() {
		File nonExistingFile = NioFileSystem.rootedAt(emptyFilesystem()) //
				.file("testFile");

		assertThat(nonExistingFile.exists(), is(false));
	}

	@Test
	public void testExistsForFileWhichIsAFolderReturnsFalse() {
		File fileWhichIsAFolder = NioFileSystem.rootedAt(testFilesystem(folder("nameOfAnExistingFolder"))) //
				.file("nameOfAnExistingFolder");

		assertThat(fileWhichIsAFolder.exists(), is(false));
	}

	@Test
	public void testLastModifiedForExistingFileReturnsLastModifiedValue() {
		Instant expectedLastModified = Instant.parse("2015-12-31T15:03:34Z");
		File existingFile = NioFileSystem
				.rootedAt(testFilesystem( //
						file("testFile").withLastModified(expectedLastModified))) //
				.file("testFile");

		assertThat(existingFile.lastModified(), is(expectedLastModified));
	}

	@Test
	public void testLastModifiedForNonExistingFileThrowsUncheckedIOExceptionWithPathInMessage() {
		Path filesystemPath = emptyFilesystem();
		Path pathOfNonExistingFile = filesystemPath.resolve("nonExistingFile");
		File nonExistingFile = NioFileSystem.rootedAt(filesystemPath) //
				.file("nonExistingFile");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(pathOfNonExistingFile.toString());

		nonExistingFile.lastModified();
	}

	@Test
	public void testLastModifiedForNonFileWhichIsAFolderThrowsUncheckedIOExceptionWithPathInMessage() {
		Path filesystemPath = testFilesystem(folder("nameOfAnExistingFolder"));
		Path pathOfNonExistingFile = filesystemPath.resolve("nameOfAnExistingFolder");
		File fileWhichIsAFolder = NioFileSystem.rootedAt(filesystemPath) //
				.file("nameOfAnExistingFolder");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(pathOfNonExistingFile.toString());

		fileWhichIsAFolder.lastModified();
	}

	@Test
	public void testCompareToReturnsZeroForSameInstance() {
		File file = NioFileSystem.rootedAt(emptyFilesystem()).file("fileName");

		assertThat(file.compareTo(file), is(0));
	}

	@Test
	public void testCompareToReturnsZeroForSameFile() {
		FileSystem filesystem = NioFileSystem.rootedAt(emptyFilesystem());
		File fileA = filesystem.file("fileName");
		File fileB = filesystem.file("fileName");

		assertThat(fileA.compareTo(fileB), is(0));
		assertThat(fileB.compareTo(fileA), is(0));
	}

	@Test
	public void testCompareToReturnsNonZeroForOtherFile() {
		FileSystem filesystem = NioFileSystem.rootedAt(emptyFilesystem());
		File fileA = filesystem.file("aFileName");
		File fileB = filesystem.file("anotherFileName");

		int compareAWithB = fileA.compareTo(fileB);
		int compareBWithA = fileB.compareTo(fileA);
		assertThat(compareAWithB, not(is(0)));
		assertThat(compareBWithA, not(is(0)));
		assertThat(signum(compareAWithB) + signum(compareBWithA), is(0));
	}

	@Test
	public void testCompareToThrowsExceptionForFileFromDifferentFileSystem() {
		File fileA = NioFileSystem.rootedAt(emptyFilesystem()).file("aFileName");
		File fileB = NioFileSystem.rootedAt(emptyFilesystem()).file("aFileName");

		thrown.expect(IllegalArgumentException.class);

		fileA.compareTo(fileB);
	}

	@Test
	public void testToString() {
		Path filesystemPath = emptyFilesystem();
		Path absoluteFilePath = filesystemPath.resolve("fileName").toAbsolutePath();
		File file = NioFileSystem.rootedAt(filesystemPath).file("fileName");

		assertThat(file.toString(), is(format("NioFile(%s)", absoluteFilePath)));
	}

	@Test
	public void testNameReturnsNameOfFile() {
		String fileName = "fileName";
		File file = NioFileSystem.rootedAt(emptyFilesystem()).file(fileName);

		assertThat(file.name(), is(fileName));
	}

	@Test
	public void testParentForDirectChildOfFileSystemReturnsFileSystem() {
		FileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());
		File file = fileSystem.file("fileName");

		assertThat(file.parent(), presentOptionalWithValueThat(is(sameInstance(fileSystem))));
	}

	@Test
	public void testParentForChildOfFolderReturnsFolder() {
		Folder folder = NioFileSystem.rootedAt(emptyFilesystem()).folder("folderName");
		File file = folder.file("fileName");

		assertThat(file.parent(), presentOptionalWithValueThat(is(sameInstance(folder))));
	}

	@Test
	public void testCopyToNonExistingTargetCreatesTargetWithContent() {
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
	public void testCopyToExistingTargetOverwritesTargetWithContent() {
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
	public void testCopyToSameFileThrowsIllegalArgumentException() {
		File file = NioFileSystem.rootedAt(testFilesystem(file("sourceFile"))).file("fileName");

		thrown.expect(IllegalArgumentException.class);

		file.copyTo(file);
	}

	@Test
	public void testCopyToDirectoryTargetThrowsUncheckedIOExceptionWithPathInMessage() {
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
	public void testCopyToOfNonExistingFileThrowsUncheckedIOExceptionWithPathInMessage() {
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
	public void testCopyToOfFileWhichIsAFolderThrowsUncheckedIOExceptionWithPathInMessage() {
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
	public void testMoveToNonExistingTargetCreatesTargetWithContentAndDeletesSource() {
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
	public void testMoveToExistingTargetOverwritesTargetWithContentAndDeletesSource() {
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
	public void testMoveToSameFileDoesNothing() {
		Path filesystemPath = testFilesystem(file("fileName").withData("fileContents"));
		Path filePath = filesystemPath.resolve("fileName");
		File file = NioFileSystem.rootedAt(filesystemPath).file("fileName");

		file.moveTo(file);

		assertThat(filePath, isFile().withContent("fileContents"));
	}

	@Test
	public void testMoveToDirectoryTargetThrowsUncheckedIOExceptionWithPathInMessage() {
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
	public void testMoveToOfNonExistingFileThrowsUncheckedIOExceptionWithPathInMessage() {
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
	public void testMoveToOfFileWhichIsAFolderThrowsUncheckedIOExceptionWithPathInMessage() {
		Path filesystemPath = testFilesystem(folder("folderName"));
		Path filePath = filesystemPath.resolve("folderName").toAbsolutePath();
		FileSystem fileSystem = NioFileSystem.rootedAt(filesystemPath);
		File fileWhichIsAFolder = fileSystem.file("folderName");
		File target = fileSystem.file("target");

		thrown.expect(UncheckedIOException.class);
		thrown.expectMessage(filePath.toString());

		fileWhichIsAFolder.moveTo(target);
	}

	public class OpenReadable {

		@Test
		public void testOpenReadableReturnsAReadableNioFileForAnExistingFile() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");

			ReadableFile result = file.openReadable();

			assertThat(result, is(instanceOf(ReadableNioFile.class)));
		}

		@Test
		public void testOpenReadableThrowsAnUncheckedIOExceptionIfTheFileDoesNotExists() {
			Path filesystemPath = emptyFilesystem();
			Path nonExistingFilePath = filesystemPath.resolve("nonExistingFile");
			File nonExistingFile = NioFileSystem.rootedAt(filesystemPath).file("nonExistingFile");

			thrown.expect(UncheckedIOException.class);
			thrown.expectMessage(nonExistingFilePath.toString());

			nonExistingFile.openReadable();
		}

		@Test
		public void testOpenReadableThrowsIllegalStateExceptionIfInvokedTwiceFromWithingTheSameThread() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openReadable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Current thread is already reading this file");

			file.openReadable();
		}

		@Test
		public void testOpenReadableDoesNotThrowIllegalStateExceptionIfInvokedTwiceFromWithingTheSameThreadButTheFirstReadableFileHasBeenClosed() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openReadable().close();

			ReadableFile result = file.openReadable();

			assertThat(result, is(instanceOf(ReadableNioFile.class)));
		}

		@Test
		public void testOpenReadableThrowsIllegalStateExceptionIfInvokedAfterOpenWritable() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Current thread is currently writing this file");

			file.openReadable();
		}

		@Test
		public void testOpenReadableDoesNotThrowIllegalStateExceptionIfInvokedAfterOpenWritableButTheWritableFileHasBeenClosed() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable().close();

			ReadableFile result = file.openReadable();

			assertThat(result, is(instanceOf(ReadableNioFile.class)));
		}

	}

	public class OpenWritable {

		@Test
		public void testOpenWritableReturnsAWritableNioFileForAnExistingFile() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");

			WritableFile result = file.openWritable();

			assertThat(result, is(instanceOf(WritableNioFile.class)));
		}

		@Test
		public void testOpenWritableReturnsAWritableNioFileForANonExisitingFile() {
			File file = NioFileSystem.rootedAt(emptyFilesystem()).file("nonExistingFile");

			WritableFile result = file.openWritable();

			assertThat(result, is(instanceOf(WritableNioFile.class)));
		}

		@Test
		public void testOpenWritableDoesNotCreateANonExisitingFile() {
			Path filesystemPath = emptyFilesystem();
			Path filePath = filesystemPath.resolve("nonExistingFile");
			File file = NioFileSystem.rootedAt(filesystemPath).file("nonExistingFile");

			file.openWritable();

			assertThat(filePath, doesNotExist());
		}

		@Test
		public void testOpenWritableThrowsIllegalStateExceptionIfInvokedTwiceFromWithingTheSameThread() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Current thread is already writing this file");

			file.openWritable();
		}

		@Test
		public void testOpenWritableDoesNotThrowIllegalStateExceptionIfInvokedTwiceFromWithingTheSameThreadButTheFirstWritableFileHasBeenClosed() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable().close();

			WritableFile result = file.openWritable();

			assertThat(result, is(instanceOf(WritableNioFile.class)));
		}

		@Test
		public void testOpenWritableThrowsIllegalStateExceptionIfInvokedAfterOpenReadable() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openReadable();

			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Current thread is currently reading this file");

			file.openWritable();
		}

		@Test
		public void testOpenWritableDoesNotThrowIllegalStateExceptionIfInvokedAfterOpenReadableButTheReadableFileHasBeenClosed() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openReadable().close();

			WritableFile result = file.openWritable();

			assertThat(result, is(instanceOf(WritableNioFile.class)));
		}

	}

	public class OpenWithMultipleThreads {

		@Rule
		public Timeout timeout = Timeout.seconds(5);

		@Test
		public void testOpenReadableInvokedInTwoThreadsCompletes() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");

			ReadableFile readableFileFromThread1 = computeInThread(file::openReadable);
			ReadableFile readableFileFromThread2 = computeInThread(file::openReadable);

			assertThat(readableFileFromThread1, is(instanceOf(ReadableNioFile.class)));
			assertThat(readableFileFromThread2, is(instanceOf(ReadableNioFile.class)));
			assertThat(readableFileFromThread1, is(not(sameInstance(readableFileFromThread2))));
		}

		@Test
		public void testOpenReadableInvokedWhileWritableFileIsOpenBlocks() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable();

			Thread thread = inThread(file::openReadable);

			assertThat(thread, is(stackContains(NioFile.class, "openReadable")));
		}

		@Test
		public void testOpenWritableInvokedWhileReadableFileIsOpenBlocks() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openReadable();

			Thread thread = inThread(file::openWritable);

			assertThat(thread, is(stackContains(NioFile.class, "openWritable")));
		}

		@Test
		public void testOpenWritableInvokedWhileWritableFileIsOpenBlocks() {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			file.openWritable();

			Thread thread = inThread(file::openWritable);

			assertThat(thread, is(stackContains(NioFile.class, "openWritable")));
		}

		@Test
		public void testOpenReadableInvokedWhileWritableFileIsOpenCompletesAfterClosingIt() throws InterruptedException {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			WritableFile writableFile = file.openWritable();
			Thread thread = inThread(file::openReadable);
			writableFile.close();

			thread.join();
		}

		@Test
		public void testOpenWritableInvokedWhileReadableFileIsOpenCompletesAfterClosingIt() throws InterruptedException {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			ReadableFile readableFile = file.openReadable();
			Thread thread = inThread(file::openWritable);
			readableFile.close();

			thread.join();
		}

		@Test
		public void testOpenWritableInvokedWhileWritableFileIsOpenCompletesAfterClosingIt() throws InterruptedException {
			File file = NioFileSystem.rootedAt(testFilesystem(file("fileName"))).file("fileName");
			WritableFile writableFile = file.openWritable();
			Thread thread = inThread(file::openWritable);
			writableFile.close();

			thread.join();
		}

		private Thread inThread(Runnable task) {
			Thread thread = new Thread(task);
			thread.start();
			try {
				// give thread time to execute work
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			return thread;
		}

		private <T> T computeInThread(Supplier<T> computation) {
			CompletableFuture<T> future = new CompletableFuture<>();
			inThread(() -> {
				future.complete(computation.get());
			});
			try {
				return future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private int signum(int value) {
		if (value > 0) {
			return 1;
		} else if (value < 0) {
			return -1;
		} else {
			return 0;
		}
	}

}
