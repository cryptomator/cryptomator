package org.cryptomator.filesystem.invariants;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.invariants.FileSystemFactories.FileSystemFactory;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class FileSystemTests {

	@DataPoints
	public static final Iterable<FileSystemFactory> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testFileSystemHasNoParent(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.parent(), is(Optional.empty()));
	}

	@Theory
	public void testFileSystemExists(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.exists(), is(true));
	}

	@Theory
	public void testFileSystemHasNoChildren(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.children().count(), is(0L));
	}

	@Theory
	public void testFileSystemHasNoFiles(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.files().count(), is(0L));
	}

	@Theory
	public void testFileSystemHasNoFolders(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.folders().count(), is(0L));
	}

	@Theory
	public void testFileSystemsFileSystemIsItself(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.fileSystem(), is(inTest));
	}

	@Theory
	public void testFileSystemBelongsToSameFilesystemWhenCheckingItself(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.belongsToSameFilesystem(inTest), is(true));
	}

	@Theory
	public void testFileSystemDoesNotBelongToSameFilesystemWhenCheckingOtherInstance(FileSystemFactory factory) {
		FileSystem inTest = factory.create();
		FileSystem otherInstance = factory.create();

		assertThat(inTest.belongsToSameFilesystem(otherInstance), is(false));
	}

	@Theory
	public void testFileSystemIsNoAncestorOfItself(FileSystemFactory factory) {
		FileSystem inTest = factory.create();

		assertThat(inTest.isAncestorOf(inTest), is(false));
	}

}
