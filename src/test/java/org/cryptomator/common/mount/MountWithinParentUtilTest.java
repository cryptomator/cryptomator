package org.cryptomator.common.mount;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static org.cryptomator.common.mount.MountWithinParentUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class MountWithinParentUtilTest {

	@Test
	void testMountPointDoesNotExist(@TempDir Path parentDir) {
		Path mount = parentDir.resolve("mount");
		assertThrows(MountPointNotExistingException.class, () -> prepareParentNoMountPoint(mount));
	}

	@Test
	void testMountPointIsFile(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Files.createFile(mount);
		assertThrows(MountPointNotEmptyDirectoryException.class, () -> prepareParentNoMountPoint(mount));
	}

	@Test
	void testMountPointIsEmptyDirectory(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Files.createDirectory(mount);
		prepareParentNoMountPoint(mount);
		assertTrue(Files.notExists(mount, NOFOLLOW_LINKS));
	}

	@Test
	void testHideawayDoesNotExist(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Path hideaway = getHideaway(mount);
		assertFalse(Files.exists(hideaway, NOFOLLOW_LINKS));
	}

	@Test
	void testHideawayExistsAsFile(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Path hideaway = getHideaway(mount);
		Files.createFile(hideaway);
		assertThrows(HideawayNotDirectoryException.class, () -> prepareParentNoMountPoint(mount));
	}

	@Test
	void testHideawayExistsAsDirectory(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Path hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		prepareParentNoMountPoint(mount);
		assertTrue(Files.notExists(mount, NOFOLLOW_LINKS));
	}

	@Test
	void testCleanupRemovesHideaway(@TempDir Path parentDir) throws IOException {
		Path mount = parentDir.resolve("mount");
		Path hideaway = getHideaway(mount);
		Files.createDirectory(hideaway);
		cleanup(mount);
		assertTrue(Files.exists(mount, NOFOLLOW_LINKS));
		assertTrue(Files.notExists(hideaway, NOFOLLOW_LINKS));
	}

	@Test
	void testGetHideawayStructure(@TempDir Path parentDir) {
		Path mount = parentDir.resolve("mount");
		Path hideaway = getHideaway(mount);
		assertEquals(mount.getParent(), hideaway.getParent());
		assertEquals(mount.getParent().resolve(".~$mount.tmp"), hideaway);
	}
}
