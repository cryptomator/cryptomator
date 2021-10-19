package org.cryptomator.common.mountpoint;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

@Singleton
class MountPointHelper {

	public static Logger LOG = LoggerFactory.getLogger(MountPointHelper.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private final Optional<Path> tmpMountPointDir;
	private volatile boolean unmountDebrisCleared = false;

	@Inject
	public MountPointHelper(Environment env) {
		this.tmpMountPointDir = env.getMountPointsDir();
	}

	public Path chooseTemporaryMountPoint(VaultSettings vaultSettings, Path parentDir) {
		String basename = vaultSettings.mountName().get();
		//regular
		Path mountPoint = parentDir.resolve(basename);
		if (Files.notExists(mountPoint)) {
			return mountPoint;
		}
		//with id
		mountPoint = parentDir.resolve(basename + " (" + vaultSettings.getId() + ")");
		if (Files.notExists(mountPoint)) {
			return mountPoint;
		}
		//with id and count
		for (int i = 1; i < MAX_TMPMOUNTPOINT_CREATION_RETRIES; i++) {
			mountPoint = parentDir.resolve(basename + "_(" + vaultSettings.getId() + ")_" + i);
			if (Files.notExists(mountPoint)) {
				return mountPoint;
			}
		}
		LOG.error("Failed to find feasible mountpoint at {}{}{}_x. Giving up after {} attempts.", parentDir, File.separator, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		return null;
	}

	public synchronized void clearIrregularUnmountDebrisIfNeeded() {
		if (unmountDebrisCleared || tmpMountPointDir.isEmpty()) {
			return; // nothing to do
		}
		if (Files.exists(tmpMountPointDir.get(), LinkOption.NOFOLLOW_LINKS)) {
			clearIrregularUnmountDebris(tmpMountPointDir.get());
		}
		unmountDebrisCleared = true;
	}

	private void clearIrregularUnmountDebris(Path dirContainingMountPoints) {
		IOException cleanupFailed = new IOException("Cleanup failed");

		try {
			LOG.debug("Performing cleanup of mountpoint dir {}.", dirContainingMountPoints);
			for (Path p : Files.newDirectoryStream(dirContainingMountPoints)) {
				try {
					var attr = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
					if (attr.isOther() && attr.isDirectory()) { // yes, this is possible with windows junction points -.-
						Files.delete(p);
					} else if (attr.isDirectory()) {
						deleteEmptyDir(p);
					} else if (attr.isSymbolicLink()) {
						deleteDeadLink(p);
					} else {
						LOG.debug("Found non-directory element in mountpoint dir: {}", p);
					}
				} catch (IOException e) {
					cleanupFailed.addSuppressed(e);
				}
			}

			if (cleanupFailed.getSuppressed().length > 0) {
				throw cleanupFailed;
			}
		} catch (IOException e) {
			LOG.warn("Unable to perform cleanup of mountpoint dir {}.", dirContainingMountPoints, e);
		} finally {
			unmountDebrisCleared = true;
		}
	}

	private void deleteEmptyDir(Path dir) throws IOException {
		assert Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS);
		try {
			ensureIsEmpty(dir);
			Files.delete(dir); // attempt to delete dir non-recursively (will fail, if there are contents)
		} catch (DirectoryNotEmptyException e) {
			LOG.info("Found non-empty directory in mountpoint dir: {}", dir);
		}
	}

	private void deleteDeadLink(Path symlink) throws IOException {
		assert Files.isSymbolicLink(symlink);
		if (Files.notExists(symlink)) { // following link: target does not exist
			Files.delete(symlink);
		}
	}

	private void ensureIsEmpty(Path dir) throws IOException {
		if (Files.newDirectoryStream(dir).iterator().hasNext()) {
			throw new DirectoryNotEmptyException(dir.toString());
		}
	}
}
