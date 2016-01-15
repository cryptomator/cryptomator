package org.cryptomator.filesystem.invariants;

import static org.cryptomator.common.test.matcher.OptionalMatcher.presentOptionalWithValueThat;
import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.fileWithName;
import static org.cryptomator.filesystem.invariants.matchers.NodeMatchers.folderWithName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.cryptomator.filesystem.invariants.FolderBiFunctions.FolderBiFunction;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FolderTests {

	private static final String FOLDER_NAME = "folderName";
	private static final String FOLDER_NAME_2 = "folderName2";

	private static final String PATH_NAME_1 = "pathName1";
	private static final String PATH_NAME_2 = "pathName2";
	private static final String PATH = PATH_NAME_1 + '/' + PATH_NAME_2;

	private static final String FILE_NAME = "fileName";

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@DataPoints
	public static final Iterable<FolderBiFunction> SUBFOLDER_FACTORIES = new FolderBiFunctions();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testFolderReturnsFolder(FileSystemFactory fileSystemFactory) {
		FileSystem fileSystem = fileSystemFactory.create();

		assertThat(fileSystem.folder(FOLDER_NAME), is(notNullValue()));
	}

	@Theory
	public void testFolderOnSubfolderReturnsFolder(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(folder.folder(FOLDER_NAME), is(notNullValue()));
	}

	@Theory
	public void testResolveFolderReturnsFolder(FileSystemFactory fileSystemFactory) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder resolvedFolder = fileSystem.resolveFolder(PATH);

		assertThat(resolvedFolder, is(folderWithName(PATH_NAME_2)));
		assertThat(resolvedFolder.parent(), presentOptionalWithValueThat(is(folderWithName(PATH_NAME_1))));
		assertThat(resolvedFolder.parent().get().parent(), presentOptionalWithValueThat(is(fileSystem)));
	}

	@Theory
	public void testResolveFolderOnSubfolderReturnsFolder(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder resolvedFolder = folder.resolveFolder(PATH);

		assertThat(resolvedFolder, is(folderWithName(PATH_NAME_2)));
		assertThat(resolvedFolder.parent(), presentOptionalWithValueThat(is(folderWithName(PATH_NAME_1))));
		assertThat(resolvedFolder.parent().get().parent(), presentOptionalWithValueThat(is(folder)));
	}

	@Theory
	public void testFileReturnsFile(FileSystemFactory fileSystemFactory) {
		FileSystem fileSystem = fileSystemFactory.create();

		assertThat(fileSystem.file(FILE_NAME), is(notNullValue()));
	}

	@Theory
	public void testFileOnSubfolderReturnsFile(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(folder.file(FILE_NAME), is(notNullValue()));
	}

	@Theory
	public void testResolveFileReturnsFile(FileSystemFactory fileSystemFactory) {
		FileSystem fileSystem = fileSystemFactory.create();

		File resolvedFile = fileSystem.resolveFile(PATH);

		assertThat(resolvedFile, is(fileWithName(PATH_NAME_2)));
		assertThat(resolvedFile.parent(), presentOptionalWithValueThat(is(folderWithName(PATH_NAME_1))));
		assertThat(resolvedFile.parent().get().parent(), presentOptionalWithValueThat(is(fileSystem)));
	}

	@Theory
	public void testResolveFileOnSubfolderReturnsFile(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		File resolvedFile = folder.resolveFile(PATH);

		assertThat(resolvedFile, is(fileWithName(PATH_NAME_2)));
		assertThat(resolvedFile.parent(), presentOptionalWithValueThat(is(folderWithName(PATH_NAME_1))));
		assertThat(resolvedFile.parent().get().parent(), presentOptionalWithValueThat(is(folder)));
	}

	@Theory
	public void testExistingFolderExists(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		assumeThat(folderBiFunction.returnedFoldersExist(), is(true));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.exists(), is(true));
	}

	@Theory
	public void testNonExistingFolderDoesntExists(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		assumeThat(folderBiFunction.returnedFoldersExist(), is(false));

		FileSystem fileSystem = fileSystemFactory.create();
		Folder existingFolder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(existingFolder.exists(), is(false));
	}

	@Theory
	public void testFolderIsNotAncecstorOfItself(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);

		assertThat(folder.isAncestorOf(folder), is(false));
	}

	@Theory
	public void testFolderIsNotAncecstorOfItsParent(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder parent = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder child = folderBiFunction.subfolderWithName(parent, FOLDER_NAME);

		assertThat(child.isAncestorOf(parent), is(false));
	}

	@Theory
	public void testFolderIsNotAncecstorOfItsParentsParent(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder parentsParent = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder parent = folderBiFunction.subfolderWithName(parentsParent, FOLDER_NAME);
		Folder child = folderBiFunction.subfolderWithName(parent, FOLDER_NAME);

		assertThat(child.isAncestorOf(parentsParent), is(false));
	}

	@Theory
	public void testFolderIsNotAncecstorOfItsSibling(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder sibling = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME_2);

		assertThat(folder.isAncestorOf(sibling), is(false));
	}

	@Theory
	public void testFolderIsAncecstorOfItsChild(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder child = folderBiFunction.subfolderWithName(folder, FOLDER_NAME);

		assertThat(folder.isAncestorOf(child), is(true));
	}

	@Theory
	public void testFolderIsAncecstorOfItsChildsChild(FileSystemFactory fileSystemFactory, FolderBiFunction folderBiFunction) {
		FileSystem fileSystem = fileSystemFactory.create();

		Folder folder = folderBiFunction.subfolderWithName(fileSystem, FOLDER_NAME);
		Folder child = folderBiFunction.subfolderWithName(folder, FOLDER_NAME);
		Folder childsChild = folderBiFunction.subfolderWithName(child, FOLDER_NAME);

		assertThat(folder.isAncestorOf(childsChild), is(true));
	}

}
