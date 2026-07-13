package org.cryptomator.launcher;

import org.cryptomator.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Unpacks a vault template (a ZIP archive holding a ready-made vault) and moves the contained vault to a destination.
 * <p>
 * The archive is expanded into a temporary directory using the Java ZIP {@link FileSystem}, and the vault directory
 * (identified by containing a {@value Constants#VAULTCONFIG_FILENAME} file) is then moved to the destination in a single
 * step, so the vault only ever appears complete at its final location.
 */
public final class VaultTemplateExtractor {

	private static final Logger LOG = LoggerFactory.getLogger(VaultTemplateExtractor.class);

	private VaultTemplateExtractor() {
	}

	/**
	 * Unpacks the given template and moves the contained vault to {@code destination}.
	 *
	 * @param template    the ZIP archive bytes
	 * @param destination the target vault directory, which must not yet exist
	 * @return {@code destination}
	 * @throws IOException if the template is not a valid ZIP, contains no vault, the destination already exists, or the
	 *                     files cannot be written or moved
	 */
	public static Path extractAndMove(byte[] template, Path destination) throws IOException {
		if (Files.exists(destination)) {
			throw new FileAlreadyExistsException(destination.toString());
		}
		Path tmpZip = Files.createTempFile("vault-template-", ".zip");
		Path tmpDir = Files.createTempDirectory("vault-template-");
		try {
			Files.write(tmpZip, template);
			Path vaultRoot = unzip(tmpZip, tmpDir);
			Path parent = destination.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			move(vaultRoot, destination);
			return destination;
		} finally {
			deleteQuietly(tmpDir);
			deleteQuietly(tmpZip);
		}
	}

	private static Path unzip(Path zipFile, Path targetDir) throws IOException {
		AtomicReference<Path> vaultConfig = new AtomicReference<>();
		Path normalizedTarget = targetDir.normalize();
		try (FileSystem zipFs = FileSystems.newFileSystem(zipFile)) {
			for (Path zipRoot : zipFs.getRootDirectories()) {
				Files.walkFileTree(zipRoot, Set.of(), 6, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						Files.createDirectories(resolveSafely(normalizedTarget, zipRoot, dir));
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						var target = resolveSafely(normalizedTarget, zipRoot, file);
						Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
						if (file.getFileName() != null && Constants.VAULTCONFIG_FILENAME.equals(file.getFileName().toString())) {
							if (!vaultConfig.compareAndSet(null, target)) {
								throw new IOException("Vault template contains more than one " + Constants.VAULTCONFIG_FILENAME);
							}
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}
		}

		if (vaultConfig.get() == null) {
			throw new IOException("Template does not contain a vault (no " + Constants.VAULTCONFIG_FILENAME + " found).");
		}
		return vaultConfig.get().getParent();
	}

	private static Path resolveSafely(Path targetDir, Path zipRoot, Path entry) throws IOException {
		Path resolved = targetDir.resolve(zipRoot.relativize(entry).toString()).normalize();
		if (!resolved.startsWith(targetDir)) {
			throw new IOException("Refusing to extract entry outside of target directory: " + entry);
		}
		return resolved;
	}

	private static void move(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
			return;
		} catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
			// provider without atomic move support - fall through to non-atomic strategies
		}
		try {
			Files.move(source, destination);
		} catch (IOException e) {
			// likely a cross-store move of a non-empty directory: copy recursively, then delete the source
			copyRecursively(source, destination);
			deleteRecursively(source);
		}
	}

	private static void copyRecursively(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(target.resolve(source.relativize(dir).toString()));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, target.resolve(source.relativize(file).toString()), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static void deleteQuietly(Path path) {
		try {
			deleteRecursively(path);
		} catch (IOException e) {
			LOG.warn("Failed to clean up temporary path {}", path, e);
		}
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		try (Stream<Path> stream = Files.walk(path)) {
			var paths = stream.sorted(Comparator.reverseOrder()).toList();
			for (Path p : paths) {
				Files.deleteIfExists(p);
			}
		}
	}

}
