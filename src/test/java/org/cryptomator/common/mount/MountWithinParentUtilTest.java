package org.cryptomator.common.mount;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static org.cryptomator.common.mount.MountWithinParentUtil.cleanup;
import static org.cryptomator.common.mount.MountWithinParentUtil.getHideaway;
import static org.cryptomator.common.mount.MountWithinParentUtil.prepareParentNoMountPoint;
import static org.cryptomator.common.mount.MountWithinParentUtil.removeResidualHideaway;
import static org.cryptomator.common.mount.MountWithinParentUtil.removeResidualJunction;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MountWithinParentUtilTest {

	@Test
	void testPrepareNeitherExist(@TempDir Path parentDir) {
		assertThrows(MountPointNotExistingException.class, () -> {
			prepareParentNoMountPoint(parentDir.resolve("mount"));
		});
	}

	@Test
	void testPrepareOnlyHideawayFileExists(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createFile(hideaway);

		assertThrows(HideawayNotDirectoryException.class, () -> {
			prepareParentNoMountPoint(mount);
		});
	}

	@Test
	void testPrepareOnlyHideawayDirExists(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		assertFalse(isHidden(hideaway));

		prepareParentNoMountPoint(mount);

		assumeTrue(SystemUtils.IS_OS_WINDOWS);
		assertTrue(isHidden(hideaway));
	}

	@Test
	void testPrepareBothExistHideawayFile(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createFile(hideaway);
		Files.createDirectory(mount);

		assertThrows(HideawayNotDirectoryException.class, () -> {
			prepareParentNoMountPoint(mount);
		});
	}

	@Test
	void testPrepareBothExistHideawayNotEmpty(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		Files.createFile(hideaway.resolve("dummy"));
		Files.createDirectory(mount);

		assertThrows(DirectoryNotEmptyException.class, () -> {
			prepareParentNoMountPoint(mount);
		});
	}

	@Test
	void testPrepareBothExistMountNotDir(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		Files.createFile(mount);

		assertThrows(MountPointNotEmptyDirectoryException.class, () -> {
			prepareParentNoMountPoint(mount);
		});
		assertTrue(Files.notExists(hideaway, NOFOLLOW_LINKS));
	}

	@Test
	void testPrepareBothExistMountNotEmpty(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		Files.createDirectory(mount);
		Files.createFile(mount.resolve("dummy"));

		assertThrows(MountPointNotEmptyDirectoryException.class, () -> {
			prepareParentNoMountPoint(mount);
		});
		assertTrue(Files.notExists(hideaway, NOFOLLOW_LINKS));
	}

	@Test
	void testPrepareBothExist(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		Files.createDirectory(mount);

		prepareParentNoMountPoint(mount);
		assertTrue(Files.notExists(mount, NOFOLLOW_LINKS));

		assumeTrue(SystemUtils.IS_OS_WINDOWS);
		assertTrue(isHidden(hideaway));
	}

	@Test
	void testRemoveResidualJunction(@TempDir Path parentDir) throws IOException {
		//Sadly can't easily create files with "Other" attribute
		var regularFile = parentDir.resolve("regularFile");
		Files.createFile(regularFile);

		assertTrue(removeResidualJunction(regularFile));
		assertFalse(removeResidualJunction(parentDir.resolve("notExisting")));
	}

	@Test
	void testRemoveResidualHideawayFile(@TempDir Path parentDir) throws IOException {
		var hideaway = parentDir.resolve("hideaway");
		Files.createFile(hideaway);

		assertThrows(HideawayNotDirectoryException.class, () -> removeResidualHideaway(parentDir.resolve("mount"), hideaway));
	}

	@Test
	void testRemoveResidualHideawayNotEmpty(@TempDir Path parentDir) throws IOException {
		var hideaway = parentDir.resolve("hideaway");
		Files.createDirectory(hideaway);
		Files.createFile(hideaway.resolve("dummy"));

		assertThrows(DirectoryNotEmptyException.class, () -> removeResidualHideaway(parentDir.resolve("mount"), hideaway));
	}

	@Test
	void testCleanupNoHideaway(@TempDir Path parentDir) {
		assertDoesNotThrow(() -> cleanup(parentDir.resolve("mount")));
	}

	@Test
	void testCleanup(@TempDir Path parentDir) throws IOException {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);

		cleanup(mount);
		assertTrue(Files.exists(mount, NOFOLLOW_LINKS));
		assertTrue(Files.notExists(hideaway, NOFOLLOW_LINKS));
		assertFalse(isHidden(mount));
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void testGetHideawayRootDirWin() {
		var mount = Path.of("C:\\mount");
		var hideaway = getHideaway(mount);

		assertEquals(mount.getParent().toAbsolutePath(), Path.of("C:\\").toAbsolutePath());
		assertEquals(mount.getParent(), hideaway.getParent());
		assertEquals(mount.getParent().resolve(".~$mount.tmp"), hideaway);
		assertEquals(mount.getParent().toAbsolutePath() + ".~$mount.tmp", hideaway.toAbsolutePath().toString());
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void testGetHideawayRootDirUnix() {
		var mount = Path.of("/mount");
		var hideaway = getHideaway(mount);

		assertEquals(mount.getParent().toAbsolutePath(), Path.of("/").toAbsolutePath());
		assertEquals(mount.getParent(), hideaway.getParent());
		assertEquals(mount.getParent().resolve(".~$mount.tmp"), hideaway);
		assertEquals(mount.getParent().toAbsolutePath() + ".~$mount.tmp", hideaway.toAbsolutePath().toString());
	}

	@Test
	void testGetHideaway(@TempDir Path parentDir) {
		var mount = parentDir.resolve("mount");
		var hideaway = getHideaway(mount);

		assertEquals(mount.getParent(), hideaway.getParent());
		assertEquals(mount.getParent().resolve(".~$mount.tmp"), hideaway);
		assertEquals(mount.getParent().toAbsolutePath() + File.separator + ".~$mount.tmp", hideaway.toAbsolutePath().toString());
	}

	private static boolean isHidden(Path path) throws IOException {
		try {
			return (boolean) Objects.requireNonNullElse(Files.getAttribute(path, "dos:hidden", NOFOLLOW_LINKS), false);
		} catch (UnsupportedOperationException | IllegalMountPointException e) {
			return false;
		}
	}
}