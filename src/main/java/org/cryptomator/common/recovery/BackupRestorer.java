package org.cryptomator.common.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import static org.cryptomator.common.Constants.MASTERKEY_BACKUP_SUFFIX;

public final class BackupRestorer {

	private static final Logger LOG = LoggerFactory.getLogger(BackupRestorer.class);

	private BackupRestorer() {}

	public static void restoreIfPresent(Path vaultPath, String filePrefix) {
		Path targetFile = vaultPath.resolve(filePrefix);

		try (Stream<Path> files = Files.list(vaultPath)) {
			files.filter(file -> isFileMatchingPattern(file.getFileName().toString(), filePrefix))
					.max((f1, f2) -> {
						try {
							FileTime time1 = Files.getLastModifiedTime(f1);
							FileTime time2 = Files.getLastModifiedTime(f2);
							return time1.compareTo(time2);
						} catch (IOException e) {
							return 0;
						}
					})
					.ifPresent(backupFile -> copyBackupFile(backupFile, targetFile));
		} catch (IOException e) {
			LOG.info("Unable to restore backup files in '{}'", vaultPath, e);
		}
	}

	private static boolean isFileMatchingPattern(String fileName, String filePrefix) {
		return fileName.startsWith(filePrefix) && fileName.endsWith(MASTERKEY_BACKUP_SUFFIX);
	}

	private static void copyBackupFile(Path backupFile, Path configPath) {
		try {
			Files.copy(backupFile, configPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOG.warn("Unable to copy backup file from '{}' to '{}'", backupFile, configPath, e);		}
	}
}
