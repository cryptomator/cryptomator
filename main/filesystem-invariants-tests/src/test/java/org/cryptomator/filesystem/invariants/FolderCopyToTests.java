package org.cryptomator.filesystem.invariants;

import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.hasContent;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
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
public class FolderCopyToTests {

	private static final String SOURCE_FOLDER_NAME = "sourceFolderName";
	private static final String TARGET_FOLDER_NAME = "targetFolderName";

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<WayToObtainAFolder> WAYS_TO_OBTAIN_A_FOLDER = new WaysToObtainAFolder();

	@DataPoints
	public static final Iterable<WayToObtainAFile> WAYS_TO_OBTAIN_A_FILE = new WaysToObtainAFile();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testCopyAnExistingFolderToANonExistingFolderCreatesTheTargetFolder(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAnExistingFolder, WayToObtainAFolder wayToObtainANonExistingFolder) {
		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));
		assumeThat(wayToObtainANonExistingFolder.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();

		Folder source = wayToObtainAnExistingFolder.folderWithName(fileSystem, SOURCE_FOLDER_NAME);
		Folder target = wayToObtainANonExistingFolder.folderWithName(fileSystem, TARGET_FOLDER_NAME);

		source.copyTo(target);

		assertThat(source.exists(), is(true));
		assertThat(target.exists(), is(true));
	}

	@Theory
	public void testCopyAnExistingFolderToANonExistingFolderWhooseParentDoesNotExistCreatesTheParentAndTargetFolder(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAnExistingFolder,
			WayToObtainAFolder wayToObtainANonExistingFolder) {
		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));
		assumeThat(wayToObtainANonExistingFolder.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();

		Folder source = wayToObtainAnExistingFolder.folderWithName(fileSystem, SOURCE_FOLDER_NAME);
		Folder parentOfTarget = wayToObtainANonExistingFolder.folderWithName(fileSystem, TARGET_FOLDER_NAME);
		Folder target = wayToObtainANonExistingFolder.folderWithName(parentOfTarget, TARGET_FOLDER_NAME);

		source.copyTo(target);

		assertThat(source.exists(), is(true));
		assertThat(parentOfTarget.exists(), is(true));
		assertThat(target.exists(), is(true));
	}

	@Theory
	public void testCopyANonExistingFolderFails(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainANonExistingFolder) {
		assumeThat(wayToObtainANonExistingFolder.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();

		Folder source = wayToObtainANonExistingFolder.folderWithName(fileSystem, SOURCE_FOLDER_NAME);
		Folder target = wayToObtainANonExistingFolder.folderWithName(fileSystem, TARGET_FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		source.copyTo(target);
	}

	@Theory
	public void testCopyAnExistingFolderToAnExistingFolderSucceeds(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAnExistingFolder) {
		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();

		Folder source = wayToObtainAnExistingFolder.folderWithName(fileSystem, SOURCE_FOLDER_NAME);
		Folder target = wayToObtainAnExistingFolder.folderWithName(fileSystem, TARGET_FOLDER_NAME);

		source.copyTo(target);

		assertThat(source.exists(), is(true));
		assertThat(target.exists(), is(true));
	}

	@Theory
	public void testCopyAFolderWithChildrenCopiesChildrenRecursive(FileSystemFactory fileSystemFactory, WayToObtainAFolder wayToObtainAnExistingFolder, WayToObtainAFile wayToObtainAnExisitingFile,
			WayToObtainAFolder wayToObtainANonExistingFolder) {

		assumeThat(wayToObtainAnExistingFolder.returnedFoldersExist(), is(true));
		assumeThat(wayToObtainANonExistingFolder.returnedFoldersExist(), is(false));
		assumeThat(wayToObtainAnExisitingFile.returnedFilesExist(), is(true));

		String childFolderName = "childFolderName";
		String childFileName = "childFileName";
		String childFoldersChildFileName = "childFoldersChildFile";
		byte[] content1 = {23, 127, 3, 10, 101};
		byte[] content2 = {43, 22, 103, 67, 51, 5, 15, 93, 33};
		FileSystem fileSystem = fileSystemFactory.create();

		Folder source = wayToObtainAnExistingFolder.folderWithName(fileSystem, SOURCE_FOLDER_NAME);
		Folder childFolder = wayToObtainAnExistingFolder.folderWithName(source, childFolderName);
		File childFile = wayToObtainAnExisitingFile.fileWithNameAndContent(source, childFileName, content1);
		File childFoldersChildFile = wayToObtainAnExisitingFile.fileWithNameAndContent(childFolder, childFoldersChildFileName, content2);

		Folder target = wayToObtainANonExistingFolder.folderWithName(fileSystem, TARGET_FOLDER_NAME);
		Folder targetChildFolder = target.folder(childFolderName);
		File targetChildFile = target.file(childFileName);
		File targetChildFoldersChildFile = targetChildFolder.file(childFoldersChildFileName);

		source.copyTo(target);

		assertThat(source.exists(), is(true));
		assertThat(childFolder.exists(), is(true));
		assertThat(childFile.exists(), is(true));
		assertThat(childFoldersChildFile.exists(), is(true));

		assertThat(target.exists(), is(true));
		assertThat(targetChildFolder.exists(), is(true));
		assertThat(targetChildFile, hasContent(content1));
		assertThat(targetChildFoldersChildFile, hasContent(content2));
	}

}
