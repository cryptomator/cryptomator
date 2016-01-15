package org.cryptomator.filesystem.invariants;

import static java.util.stream.Collectors.toList;
import static org.cryptomator.common.test.matcher.ContainsMatcher.containsInAnyOrder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.invariants.SubfolderFactories.SubfolderFactory;
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
	public static final Iterable<SubfolderFactory> SUBFOLDER_FACTORIES = new SubfolderFactories();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testChildrenThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.children();
	}

	@Theory
	public void testFilesThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.files();
	}

	@Theory
	public void testFoldersThrowsExceptionIfFolderDoesNotExist(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder nonExistingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		thrown.expect(UncheckedIOException.class);

		nonExistingFolder.folders();
	}

	@Theory
	public void testChildrenIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.children().count(), is(0L));
	}

	@Theory
	public void testFilesIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.files().count(), is(0L));
	}

	@Theory
	public void testFoldersIsEmptyForEmptyFolder(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.folders().count(), is(0L));
	}

	@Theory
	public void testChildrenContainsCreatedChildFolder(FileSystemFactory fileSystemFactory, SubfolderFactory existingFolderFactory, SubfolderFactory childExistingFolderFactory) {
		assumeThat(existingFolderFactory.createsExistingFolder(), is(true));
		assumeThat(childExistingFolderFactory.createsExistingFolder(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFactory.subfolderWithName(existingFolder, childName);

		assertThat(existingFolder.children().collect(toList()), containsInAnyOrder(equalTo(childFolder)));
	}

	@Theory
	public void testChildrenDoesNotContainsCreatedAndDeletedChildFolder(FileSystemFactory fileSystemFactory, SubfolderFactory existingFolderFactory, SubfolderFactory childExistingFolderFactory) {
		assumeThat(existingFolderFactory.createsExistingFolder(), is(true));
		assumeThat(childExistingFolderFactory.createsExistingFolder(), is(true));

		String childName = "childFolderName";

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = existingFolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder childFolder = childExistingFolderFactory.subfolderWithName(existingFolder, childName);
		childFolder.delete();

		assertThat(existingFolder.children().collect(toList()), is(empty()));
	}

}
