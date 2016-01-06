package org.cryptomator.filesystem.invariants;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.common.base.Supplier;

@RunWith(Theories.class)
public class FileSystemTests {

	@DataPoints
	public static final Iterable<Supplier<FileSystem>> FILE_SYSTEM_FACTORIES = new FileSystemFactories();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Theory
	public void testFileSystemHasNoParent(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.parent(), is(Optional.empty()));
	}

	@Theory
	public void testFileSystemExists(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.exists(), is(true));
	}

	@Theory
	public void testFileSystemHasNoChildren(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.children().count(), is(0L));
	}

	@Theory
	public void testFileSystemHasNoFiles(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.files().count(), is(0L));
	}

	@Theory
	public void testFileSystemHasNoFolders(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.folders().count(), is(0L));
	}

	@Theory
	public void testFileSystemsFileSystemIsItself(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.fileSystem(), is(inTest));
	}

	@Theory
	public void testFileSystemBelongsToSameFilesystemWhenCheckingItself(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.belongsToSameFilesystem(inTest), is(true));
	}

	@Theory
	public void testFileSystemDoesNotBelongToSameFilesystemWhenCheckingOtherInstance(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();
		FileSystem otherInstance = factory.get();

		assertThat(inTest.belongsToSameFilesystem(otherInstance), is(false));
	}

	@Theory
	public void testFileSystemIsNoAncestorOfItself(Supplier<FileSystem> factory) {
		FileSystem inTest = factory.get();

		assertThat(inTest.isAncestorOf(inTest), is(false));
	}

}
