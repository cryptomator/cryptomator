package org.cryptomator.common.mountpoint;

import org.cryptomator.common.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

@Singleton
class IrregularUnmountCleaner {

	public static Logger LOG = LoggerFactory.getLogger(IrregularUnmountCleaner.class);

	private final Optional<Path> tmpMountPointDir;
	private volatile boolean alreadyChecked = false;

	@Inject
	public IrregularUnmountCleaner(Environment env) {
		this.tmpMountPointDir = env.getMountPointsDir();
	}


	public void clearIrregularUnmountDebrisIfNeeded() {
		if (alreadyChecked || tmpMountPointDir.isEmpty()) {
			return; //nuthin to do
		}
		if (Files.exists(tmpMountPointDir.get(), LinkOption.NOFOLLOW_LINKS)) {
			clearIrregularUnmountDebris(tmpMountPointDir.get());
		}
		alreadyChecked = true;
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
			alreadyChecked = true;
		}
	}

	private void deleteEmptyDir(Path dir) throws IOException {
		assert Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS);
		try {
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

}
