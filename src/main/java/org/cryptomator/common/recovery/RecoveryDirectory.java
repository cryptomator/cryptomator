package org.cryptomator.common.recovery;

import java.io.IOException;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RecoveryDirectory implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryDirectory.class);

	private final Path recoveryPath;
	private final Path vaultPath;

	private RecoveryDirectory(Path vaultPath, Path recoveryPath) {
		this.vaultPath = vaultPath;
		this.recoveryPath = recoveryPath;
	}

	public static RecoveryDirectory create(Path vaultPath) throws IOException {
		FileAttribute<?> attr = PosixFilePermissions.asFileAttribute(
				PosixFilePermissions.fromString("rwx------")
		);
		Path tempDir = Files.createTempDirectory("cryptomator", attr);
		return new RecoveryDirectory(vaultPath, tempDir);
	}

	public void moveRecoveredFile(String file) throws IOException {
		Files.move(recoveryPath.resolve(file), vaultPath.resolve(file), StandardCopyOption.REPLACE_EXISTING);
	}

	private void deleteRecoveryDirectory() {
		try (var paths = Files.walk(recoveryPath)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					LOG.info("Unable to delete {}. Please delete it manually.", p);
				}
			});
		} catch (IOException e) {
			LOG.error("Failed to clean up recovery directory", e);
		}
	}

	@Override
	public void close() {
		deleteRecoveryDirectory();
	}

	public Path getRecoveryPath() {
		return recoveryPath;
	}

}
