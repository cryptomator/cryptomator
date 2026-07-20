package org.cryptomator.ui.importtemplate;

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
import java.util.stream.Stream;
import java.util.zip.ZipException;

/**
 * Unpacks a vault template (a ZIP archive holding a ready-made vault) and moves the contained vault to a destination.
 * <p>
 * The archive is expanded into a temporary directory using the Java ZIP {@link FileSystem}, and the vault directory
 * (identified by containing a {@value Constants#VAULTCONFIG_FILENAME} file) is then moved to the destination in a single
 * step, so the vault only ever appears complete at its final location.
 */
public final class VaultTemplateExtractor {

	private static final Logger LOG = LoggerFactory.getLogger(VaultTemplateExtractor.class);
	// On Windows, URI template (base64url encoded) is is given on command line argument. Windows API restrict the length
	// to 32,767 chars (approx. ~24KB template size).
	// Note that larger links are dropped by the OS anyway, but we also set the accepted size uniform across platforms
	static final long MAX_TOTAL_BYTES = 24_000; // ~24 KB, see above
	static final int MAX_ENTRIES = 30; // files + directories below the zip root

	private VaultTemplateExtractor() {
	}

	/**
	 * Unpacks the given template and moves the contained vault to {@code destination}.
	 *
	 * @param template    the ZIP archive bytes
	 * @param destination the target vault directory, which must not yet exist
	 * @return {@code destination}
	 * @throws MalformedTemplateException if the template is not a valid zip or does not contain a vault
	 * @throws IOException if the destination already exists, or the
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
		Path normalizedTarget = targetDir.normalize();
		try (FileSystem zipFs = FileSystems.newFileSystem(zipFile)) {
			Path zipRoot  = zipFs.getRootDirectories().iterator().next(); //we take the first available root and ignore others
			var templateExtractor = new TemplateExtractionVisitor(zipRoot, normalizedTarget, MAX_ENTRIES, MAX_TOTAL_BYTES);
			Files.walkFileTree(zipRoot, templateExtractor);
			var vaultConfig = templateExtractor.getVaultConfig();
			if (vaultConfig == null) {
				throw new MalformedTemplateException("Template does not contain a vault (no " + Constants.VAULTCONFIG_FILENAME + " found).");
			}
			return vaultConfig.getParent();
		} catch (ZipException e) {
			throw new MalformedTemplateException("Template is not a readable ZIP archive.", e);
		}
	}

	static class TemplateExtractionVisitor extends SimpleFileVisitor<Path> {

		private final Path zipRoot;
		private final Path target;
		private final int maxEntries;
		private final long maxSize;

		private int totalEntries = 0;
		private long totalBytes = 0;
		private Path vaultConfig = null;

		TemplateExtractionVisitor(Path zipRoot, Path target, int maxEntries, long maxSize) {
			this.zipRoot = zipRoot;
			this.target = target;
			this.maxEntries = maxEntries;
			this.maxSize = maxSize;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (!dir.equals(zipRoot)) { //the zip root is not part of the template's content
				countEntry();
			}
			Files.createDirectories(resolveSafely(target, zipRoot, dir));
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			countEntry();
			totalBytes += attrs.size();
			if (totalBytes > maxSize) {
				throw new MalformedTemplateException("Vault template exceeds the maximum allowed size of " + maxSize + " bytes.");
			}

			var actualTarget = Files.copy(file, resolveSafely(target, zipRoot, file), StandardCopyOption.REPLACE_EXISTING);

			if (Constants.VAULTCONFIG_FILENAME.equals(file.getFileName().toString())) {
				if (vaultConfig != null) {
					throw new MalformedTemplateException("Vault template contains more than one " + Constants.VAULTCONFIG_FILENAME);
				}
				vaultConfig = actualTarget;
			}
			return FileVisitResult.CONTINUE;
		}

		private void countEntry() throws IOException {
			totalEntries++;
			if ( totalEntries > maxEntries) {
				throw new MalformedTemplateException("Vault template contains more than the maximum allowed " + maxEntries + " entries.");
			}
		}

		Path getVaultConfig() {
			return vaultConfig;
		}
	}

	private static Path resolveSafely(Path targetDir, Path zipRoot, Path entry) throws IOException {
		Path resolved = targetDir.resolve(zipRoot.relativize(entry).toString()).normalize();
		if (!resolved.startsWith(targetDir)) {
			throw new MalformedTemplateException("Refusing to extract entry outside of target directory: " + entry);
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

	public static class MalformedTemplateException extends IOException {
		private static final long serialVersionUID = 1L;

		public MalformedTemplateException(String message) {
			super(message);
		}

		public MalformedTemplateException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
