package org.cryptomator.filesystem.nio;

import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.PathMatcher.isDirectory;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFileSystemIntegrationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testParentIsEmpty() throws IOException {
		NioFileSystem fileSystem = NioFileSystem.rootedAt(emptyFilesystem());

		assertThat(fileSystem.parent().isPresent(), is(false));
	}

	@Test
	public void testObtainingAFileSystemWithNonExistingRootCreatesItIncludingAllParentFolders() throws IOException {
		Path emptyFilesystem = emptyFilesystem();
		Path nonExistingFilesystemRoot = emptyFilesystem.resolve("nonExistingRoot");
		Files.delete(emptyFilesystem);

		NioFileSystem.rootedAt(nonExistingFilesystemRoot);

		assertThat(nonExistingFilesystemRoot, isDirectory());
	}

	@Test
	public void testObtainingAFileSystemWhooseRootIsAFileFails() throws IOException {
		Path emptyFilesystem = testFilesystem(file("rootWhichIsAFile"));
		Path rootWhichIsAFile = emptyFilesystem.resolve("rootWhichIsAFile");

		thrown.expect(UncheckedIOException.class);

		NioFileSystem.rootedAt(rootWhichIsAFile);
	}

}