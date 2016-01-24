package org.cryptomator.common.test;

import static java.nio.file.Files.walkFileTree;
import static java.util.Collections.synchronizedSet;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempFilesRemovedOnShutdown {

	private static final Logger LOG = LoggerFactory.getLogger(TempFilesRemovedOnShutdown.class);

	private static final Set<Path> PATHS_TO_REMOVE_ON_SHUTDOWN = synchronizedSet(new HashSet<>());
	private static final Thread ON_SHUTDOWN_DELETER = new Thread(TempFilesRemovedOnShutdown::removeAll);

	static {
		Runtime.getRuntime().addShutdownHook(ON_SHUTDOWN_DELETER);
	}

	public static Path createTempDirectory(String prefix) throws IOException {
		Path path = Files.createTempDirectory(prefix);
		PATHS_TO_REMOVE_ON_SHUTDOWN.add(path);
		return path;
	}

	private static void removeAll() {
		PATHS_TO_REMOVE_ON_SHUTDOWN.forEach(TempFilesRemovedOnShutdown::remove);
	}

	private static void remove(Path path) {
		try {
			tryRemove(path);
		} catch (Throwable e) {
			LOG.debug("Failed to remove " + path, e);
		}
	}

	private static void tryRemove(Path path) throws IOException {
		walkFileTree(path, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
