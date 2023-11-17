package org.cryptomator.common.mount;

import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public final class MountWithinParentUtil {

	private static final Logger LOG = LoggerFactory.getLogger(Mounter.class);
	private static final String HIDEAWAY_PREFIX = ".~$";
	private static final String HIDEAWAY_SUFFIX = ".tmp";
	private static final String WIN_HIDDEN_ATTR = "dos:hidden";

	private MountWithinParentUtil() {}

	// extracting methods to resolve the complex method smell
	static void prepareParentNoMountPoint(Path mountPoint) throws IllegalMountPointException, IOException {
		Path hideaway = getHideaway(mountPoint);
		var mpState = getMountPointState(mountPoint);
		var hideExists = Files.exists(hideaway, LinkOption.NOFOLLOW_LINKS);

		if (mpState == MountPointState.BROKEN_JUNCTION) {
			handleBrokenJunction(mountPoint);
		} else if (mpState == MountPointState.NOT_EXISTING) {
			handleNotExistingMountPoint(mountPoint, hideExists, hideaway);
		} else {
			assert mpState == MountPointState.EMPTY_DIR;
			handleExistingEmptyDirectory(mountPoint, hideaway, hideExists);
		}
	}

	private static void handleBrokenJunction(Path mountPoint) throws IOException {
		LOG.info("Mountpoint \"{}\" is still a junction. Deleting it.", mountPoint);
		Files.delete(mountPoint); // Throws if mountPoint is also a non-empty folder
	}

	private static void handleNotExistingMountPoint(Path mountPoint, boolean hideExists, Path hideaway) throws IOException {
		if (!hideExists) {
			throw new MountPointNotExistingException(mountPoint);
		}

		checkIsHideawayDirectory(mountPoint, hideaway);
		LOG.info("Mountpoint {} seems to be not properly cleaned up. Will be fixed on unmount.");
		if (SystemUtils.IS_OS_WINDOWS) {
			Files.setAttribute(hideaway, WIN_HIDDEN_ATTR, true, LinkOption.NOFOLLOW_LINKS);
		}
	}

	private static void handleExistingEmptyDirectory(Path mountPoint, Path hideaway, boolean hideExists) throws IOException {
		try {
			if (hideExists) {
				removeResidualHideaway(mountPoint, hideaway);
			}

			moveMountPointToHideaway(mountPoint, hideaway);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	private static void moveMountPointToHideaway(Path mountPoint, Path hideaway) throws IOException, InterruptedException {
		Files.move(mountPoint, hideaway);
		if (SystemUtils.IS_OS_WINDOWS) {
			Files.setAttribute(hideaway, WIN_HIDDEN_ATTR, true, LinkOption.NOFOLLOW_LINKS);
		}

		int attempts = 0;
		while (!Files.notExists(mountPoint)) {
			if (attempts >= 10) {
				throw new MountPointCleanupFailedException(mountPoint);
			}
			Thread.sleep(1000);
			attempts++;
		}
	}


	@VisibleForTesting
	static MountPointState getMountPointState(Path path) throws IOException, IllegalMountPointException {
		if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) {
			return MountPointState.NOT_EXISTING;
		}
		if (!Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther()) {
			checkIsMountPointDirectory(path);
			checkIsMountPointEmpty(path);
			return MountPointState.EMPTY_DIR;
		}
		if (Files.exists(path /* FOLLOW_LINKS */)) { //Both junction and target exist
			throw new MountPointInUseException(path);
		}
		return MountPointState.BROKEN_JUNCTION;
	}

	@VisibleForTesting
	enum MountPointState {

		NOT_EXISTING,

		EMPTY_DIR,

		BROKEN_JUNCTION;

	}

	@VisibleForTesting
	static void removeResidualHideaway(Path mountPoint, Path hideaway) throws IOException {
		checkIsHideawayDirectory(mountPoint, hideaway);
		Files.delete(hideaway); //Fails if not empty
	}

	static void cleanup(Path mountPoint) {
		Path hideaway = getHideaway(mountPoint);
		try {
			waitForMountpointRestoration(mountPoint);
			if (Files.notExists(hideaway, LinkOption.NOFOLLOW_LINKS)) {
				LOG.error("Unable to restore hidden directory to mountpoint \"{}\": Directory does not exist.", mountPoint);
				return;
			}

			Files.move(hideaway, mountPoint);
			if (SystemUtils.IS_OS_WINDOWS) {
				Files.setAttribute(mountPoint, WIN_HIDDEN_ATTR, false);
			}
		} catch (IOException e) {
			LOG.error("Unable to restore hidden directory to mountpoint \"{}\".", mountPoint, e);
		}
	}

	//on Windows removing the mountpoint takes some time, so we poll for at most 3 seconds
	private static void waitForMountpointRestoration(Path mountPoint) throws FileAlreadyExistsException {
		int attempts = 0;
		while (!Files.notExists(mountPoint, LinkOption.NOFOLLOW_LINKS)) {
			attempts++;
			if (attempts >= 5) {
				throw new FileAlreadyExistsException("Timeout waiting for mountpoint cleanup for " + mountPoint + " .");
			}

			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new FileAlreadyExistsException("Interrupted before mountpoint " + mountPoint + " was cleared");
			}
		}
	}

	private static void checkIsMountPointDirectory(Path toCheck) throws IllegalMountPointException {
		if (!Files.isDirectory(toCheck, LinkOption.NOFOLLOW_LINKS)) {
			throw new MountPointNotEmptyDirectoryException(toCheck, "Mountpoint is not a directory: " + toCheck);
		}
	}

	private static void checkIsHideawayDirectory(Path mountPoint, Path hideawayToCheck) {
		if (!Files.isDirectory(hideawayToCheck, LinkOption.NOFOLLOW_LINKS)) {
			throw new HideawayNotDirectoryException(mountPoint, hideawayToCheck);
		}
	}

	private static void checkIsMountPointEmpty(Path toCheck) throws IllegalMountPointException, IOException {
		try (var dirStream = Files.list(toCheck)) {
			if (dirStream.findFirst().isPresent()) {
				throw new MountPointNotEmptyDirectoryException(toCheck, "Mountpoint directory is not empty: " + toCheck);
			}
		}
	}

	@VisibleForTesting
	static Path getHideaway(Path mountPoint) {
		return mountPoint.resolveSibling(HIDEAWAY_PREFIX + mountPoint.getFileName().toString() + HIDEAWAY_SUFFIX);
	}

}
