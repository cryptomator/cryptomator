package org.cryptomator.filesystem.invariants;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

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
public class FolderTests {

	private static final String FOLDER_NAME = "folderName";

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<SubfolderFactory> SUBFOLDER_FACTORIES = new SubfolderFactories();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testExistingFolderExists(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.exists(), is(true));
	}

	@Theory
	public void testNonExistingFolderDoesntExists(FileSystemFactory fileSystemFactory, SubfolderFactory subfolderFactory) {
		assumeThat(subfolderFactory.createsExistingFolder(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = subfolderFactory.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.exists(), is(false));
	}

}
