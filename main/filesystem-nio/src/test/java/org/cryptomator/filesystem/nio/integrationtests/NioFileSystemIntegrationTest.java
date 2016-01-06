package org.cryptomator.filesystem.nio.integrationtests;

import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.emptyFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.file;
import static org.cryptomator.filesystem.nio.integrationtests.FilesystemSetupUtils.testFilesystem;
import static org.cryptomator.filesystem.nio.integrationtests.PathMatcher.isDirectory;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cryptomator.filesystem.nio.NioFileSystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NioFileSystemIntegrationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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