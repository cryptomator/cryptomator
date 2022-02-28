package org.cryptomator.common.mountpoint;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomMountPointChooserTest {

	//--- Mocks ---
	VaultSettings vaultSettings;
	Environment environment;

	CustomMountPointChooser customMpc;


	@BeforeEach
	public void init() {
		this.vaultSettings = Mockito.mock(VaultSettings.class);
		this.environment = Mockito.mock(Environment.class);
		this.customMpc = new CustomMountPointChooser(vaultSettings, environment);
	}

	@Nested
	class WinfspPreperations {

		@Test
		@DisplayName("Test MP preparation for winfsp, if only mountpoint is present")
		public void testPrepareParentNoMountpointOnlyMountpoint(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			Files.createDirectory(mntPoint);

			//execute
			Assertions.assertDoesNotThrow(() -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.notExists(mntPoint));

			Path hideaway = customMpc.getHideaway(mntPoint);
			Assertions.assertTrue(Files.exists(hideaway));
			Assertions.assertNotEquals(hideaway.getFileName().toString(), "mntPoint");
			Assertions.assertTrue(hideaway.getFileName().toString().contains("mntPoint"));

			Assumptions.assumeTrue(OS.WINDOWS.isCurrentOs());
			Assertions.assertTrue((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
		}

		@Test
		@DisplayName("Test MP preparation for winfsp, if only non-empty mountpoint is present")
		public void testPrepareParentNoMountpointOnlyNonEmptyMountpoint(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			Files.createDirectory(mntPoint);
			Files.createFile(mntPoint.resolve("foo"));

			//execute
			Assertions.assertThrows(InvalidMountPointException.class, () -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.exists(mntPoint.resolve("foo")));
		}

		@Test
		@DisplayName("Test MP preparation for Winfsp, if for any reason only hideaway dir is present")
		public void testPrepareParentNoMountpointOnlyHideaway(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);
			Files.createDirectory(hideaway); //we explicitly do not set the file attributes here

			//execute
			Assertions.assertDoesNotThrow(() -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.exists(hideaway));
			Assertions.assertNotEquals(hideaway.getFileName().toString(), "mntPoint");
			Assertions.assertTrue(hideaway.getFileName().toString().contains("mntPoint"));

			Assumptions.assumeTrue(OS.WINDOWS.isCurrentOs());
			Assertions.assertTrue((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
		}

		@Test
		@DisplayName("Test Winfsp MP preparation, if mountpoint and hideaway dirs are present")
		public void testPrepareParentNoMountpointMountPointAndHideaway(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);
			Files.createDirectory(hideaway); //we explicitly do not set the file attributes here
			Files.createDirectory(mntPoint);

			//execute
			Assertions.assertThrows(InvalidMountPointException.class, () -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.exists(hideaway));
			Assertions.assertTrue(Files.exists(mntPoint));

			Assumptions.assumeTrue(OS.WINDOWS.isCurrentOs());
			Assertions.assertFalse((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
		}

		@Test
		@DisplayName("Test Winfsp MP preparation, if neither mountpoint nor hideaway dir is present")
		public void testPrepareParentNoMountpointNothing(@TempDir Path tmpDir) {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);

			//execute
			Assertions.assertThrows(InvalidMountPointException.class, () -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.notExists(hideaway));
			Assertions.assertTrue(Files.notExists(mntPoint));
		}

	}


}
