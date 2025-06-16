package org.cryptomator.common.recovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;

public final class RecoveryDirectory implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryDirectory.class);

	private final Path recoveryPath;
	private final Path vaultPath;

	private RecoveryDirectory(Path vaultPath, Path recoveryPath) {
		this.vaultPath = vaultPath;
		this.recoveryPath = recoveryPath;
	}

	public static RecoveryDirectory create(Path vaultPath) throws IOException {
		Path tempDir = Files.createTempDirectory("r");
		return new RecoveryDirectory(vaultPath, tempDir);
	}

	public void moveRecoveredFiles() throws IOException {
		Files.move(recoveryPath.resolve(MASTERKEY_FILENAME), vaultPath.resolve(MASTERKEY_FILENAME), StandardCopyOption.REPLACE_EXISTING);
		Files.move(recoveryPath.resolve(VAULTCONFIG_FILENAME), vaultPath.resolve(VAULTCONFIG_FILENAME), StandardCopyOption.REPLACE_EXISTING);
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
