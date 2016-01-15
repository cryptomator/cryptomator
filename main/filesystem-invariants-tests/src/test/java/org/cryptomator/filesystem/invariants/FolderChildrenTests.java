package org.cryptomator.filesystem.invariants;

import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.containsInAnyOrder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.invariants.FileBiFunctions.FileBiFunction;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.invariants.FolderBiFunctions.FolderBiFunction;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FolderChildrenTests {

	private static final String FOLDER_NAME = "folderName";

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<FolderBiFunction> SUBFOLDER_BI_FUNCTIONS = new FolderBiFunctions();

	@DataPoints
	public static final Iterable<FileBiFunction> SUBFILE_BI_FUNCTIONS = new FileBiFunctions();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testChildrenThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.children();
	}

	@Theory
	public void testFilesThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.files();
	}

	@Theory
	public void testFoldersThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.folders();
	}

	@Theory
	public void testChildrenIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.children().count(), is(0L));
	}

	@Theory
	public void testFilesIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.files().count(), is(0L));
	}

	@Theory
	public void testFoldersIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, FolderBiFunction folderFunction) {
		assumeThat(folderFunction.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = folderFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.folders().count(), is(0L));
	}

	@Theory
	public void testChildrenContainsCreatedChildFolder(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FolderBiFunction childExistingFolderFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(childExistingFolderFunction.returnedFoldersExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFunction.subfolderWithName(existingFolder, childName);

		assertThat(existingFolder.children().collect(toList()), containsInAnyOrder(equalTo(childFolder)));
	}

	@Theory
	public void testChildrenDoesNotContainCreatedAndDeletedChildFolder(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FolderBiFunction childExistingFolderFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(childExistingFolderFunction.returnedFoldersExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFunction.subfolderWithName(existingFolder, childName);
		childFolder.delete();

		assertThat(existingFolder.children().collect(toList()), is(empty()));
	}

	@Theory
	public void testChildrenContainsCreatedFile(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FileBiFunction existingFileFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(existingFileFunction.returnedFilesExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		File file = existingFileFunction.fileWithName(existingFolder, childName);

		assertThat(existingFolder.children().collect(toList()), containsInAnyOrder(equalTo(file)));
	}

	@Theory
	public void testChildrenDoesNotContainCreatedAndDeletedFile(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FileBiFunction existingFileFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(existingFileFunction.returnedFilesExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		File file = existingFileFunction.fileWithName(existingFolder, childName);
		file.delete();

		assertThat(existingFolder.children().collect(toList()), is(empty()));
	}

	@Theory
	public void testFoldersDoesNotContainAndFilesContainsCreatedFile(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FileBiFunction existingFileFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(existingFileFunction.returnedFilesExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		File file = existingFileFunction.fileWithName(existingFolder, childName);

		assertThat(existingFolder.folders().collect(toList()), is(empty()));
		assertThat(existingFolder.files().collect(toList()), containsInAnyOrder(equalTo(file)));
	}

	@Theory
	public void testFoldersAndFilesDoesNotContainCreatedAndDeletedFile(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FileBiFunction existingFileFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(existingFileFunction.returnedFilesExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		File file = existingFileFunction.fileWithName(existingFolder, childName);
		file.delete();

		assertThat(existingFolder.folders().collect(toList()), is(empty()));
		assertThat(existingFolder.files().collect(toList()), is(empty()));
	}

	@Theory
	public void testFoldersContainsAndFilesDoesNotContainCreatedChildFolder(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FolderBiFunction childExistingFolderFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(childExistingFolderFunction.returnedFoldersExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFunction.subfolderWithName(existingFolder, childName);

		assertThat(existingFolder.folders().collect(toList()), containsInAnyOrder(equalTo(childFolder)));
		assertThat(existingFolder.files().collect(toList()), is(empty()));
	}

	@Theory
	public void testFoldersAndFilesDoesNotContainCreatedAndDeletedChildFolder(FileSystemFactory fileSystemFactory, FolderBiFunction existingFolderFunction, FolderBiFunction childExistingFolderFunction) {
		assumeThat(existingFolderFunction.returnedFoldersExist(), is(true));
		assumeThat(childExistingFolderFunction.returnedFoldersExist(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFunction.subfolderWithName(existingFolder, childName);
		childFolder.delete();

		assertThat(existingFolder.folders().collect(toList()), is(empty()));
		assertThat(existingFolder.files().collect(toList()), is(empty()));
	}

}
