package org.cryptomator.common.mountpoint;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Volume;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CustomMountPointChooserTest {

	//--- Mocks ---
	VaultSettings vaultSettings;
	Environment environment;
	Volume volume;

	CustomMountPointChooser customMpc;


	@BeforeEach
	public void init() {
		this.volume = Mockito.mock(Volume.class);
		this.vaultSettings = Mockito.mock(VaultSettings.class);
		this.environment = Mockito.mock(Environment.class);
		this.customMpc = new CustomMountPointChooser(vaultSettings);
	}

	@Nested
	public class WinfspPreperations {

		@Test
		@DisplayName("Hideaway name for PARENT_NO_MOUNTPOINT is not the same as mountpoint")
		public void testGetHideaway() {
			//prepare
			Path mntPoint = Path.of("/foo/bar");
			//execute
			var hideaway = customMpc.getHideaway(mntPoint);
			//eval
			Assertions.assertNotEquals(hideaway.getFileName(), mntPoint.getFileName());
			Assertions.assertEquals(hideaway.getParent(), mntPoint.getParent());
			Assertions.assertTrue(hideaway.getFileName().toString().contains(mntPoint.getFileName().toString()));
		}

		@Test
		@DisplayName("PARENT_NO_MOUNTPOINT preparations succeeds, if only mountpoint is present")
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

			if(OS.WINDOWS.isCurrentOs()) {
				Assertions.assertTrue((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
			}
		}

		@Test
		@DisplayName("PARENT_NO_MOUNTPOINT preparations fail, if only non-empty mountpoint is present")
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
		@DisplayName("PARENT_NO_MOUNTPOINT preparation succeeds, if for any reason only hideaway dir is present")
		public void testPrepareParentNoMountpointOnlyHideaway(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);
			Files.createDirectory(hideaway); //we explicitly do not set the file attributes here

			//execute
			Assertions.assertDoesNotThrow(() -> customMpc.prepareParentNoMountPoint(mntPoint));

			//evaluate
			Assertions.assertTrue(Files.exists(hideaway));

			if(OS.WINDOWS.isCurrentOs()) {
				Assertions.assertTrue((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
			}
		}

		@Test
		@DisplayName("PARENT_NO_MOUNTPOINT preparation fails, if mountpoint and hideaway dirs are present")
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

			if(OS.WINDOWS.isCurrentOs()) {
				Assertions.assertFalse((Boolean) Files.getAttribute(hideaway, "dos:hidden"));
			}
		}

		@Test
		@DisplayName("PARENT_NO_MOUNTPOINT preparation fails, if neither mountpoint nor hideaway dir is present")
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

		@Test
		@DisplayName("Normal Cleanup for PARENT_NO_MOUNTPOINT")
		public void testCleanupSuccess(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);

			Files.createDirectory(hideaway);
			Mockito.when(volume.getMountPointRequirement()).thenReturn(MountPointRequirement.PARENT_NO_MOUNT_POINT);

			//execute
			Assertions.assertDoesNotThrow(() -> customMpc.cleanup(volume, mntPoint));

			//evaluate
			Assertions.assertTrue(Files.exists(mntPoint));
			Assertions.assertTrue(Files.notExists(hideaway));

			if(OS.WINDOWS.isCurrentOs()) {
				Assertions.assertFalse((Boolean) Files.getAttribute(mntPoint, "dos:hidden"));
			}
		}

		@Test
		@DisplayName("On IOException cleanup for PARENT_NO_MOUNTPOINT exits normally")
		public void testCleanupIOFailure(@TempDir Path tmpDir) throws IOException {
			//prepare
			var mntPoint = tmpDir.resolve("mntPoint");
			var hideaway = customMpc.getHideaway(mntPoint);

			Files.createDirectory(hideaway);
			Mockito.when(volume.getMountPointRequirement()).thenReturn(MountPointRequirement.PARENT_NO_MOUNT_POINT);
			try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
				filesMock.when(() -> Files.move(Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(new IOException("error"));
				//execute
				Assertions.assertDoesNotThrow(() -> customMpc.cleanup(volume, mntPoint));
			}
		}

	}


}
