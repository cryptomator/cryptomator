package org.cryptomator.common.mountpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

public class UnregularUnmountCleaner {

	public static Logger LOG = LoggerFactory.getLogger(UnregularUnmountCleaner.class);

	public static void removeUnregularUnmountDebris(Path dirOfMountPoints) {
		Collection<IOException> exceptionCatcher = new ArrayList<>();
		try {
			LOG.debug("Performing cleanup of mountpoint dir {}.", dirOfMountPoints);
			Files.list(dirOfMountPoints).filter(p -> {
				if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
					return true;
				} else {
					LOG.debug("Found non-directory element in mountpoint dir: {}", p);
					return false;
				}
			}).forEach(dir -> {
				try {
					try {
						Files.readAttributes(dir, BasicFileAttributes.class); //we follow the link and see if it exists
					} catch (NoSuchFileException e) {
						Files.delete(dir); //broken link file, we delete it
						LOG.debug("Removed broken entry {} from mountpoint dir.");
						return;
					}
					//check if dir is empty
					if (Files.list(dir).findFirst().isEmpty()) {
						Files.delete(dir);
						LOG.debug("Removed empty dir {} from mountpoint dir.");
					} else {
						LOG.debug("Found non-empty directory in mountpoint dir: {}", dir);
					}
				} catch (IOException e) {
					exceptionCatcher.add(e);
				}
			});

			exceptionCatcher.forEach(exception -> {
				LOG.debug("Unable to clean element from mountpoint dir due to", exception);
			});
		} catch (IOException e) {
			LOG.debug("Unable to perform cleanup of mountpoint dir {}.", dirOfMountPoints, e);
		}

	}

}
