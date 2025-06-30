package org.cryptomator.common.recovery;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import static org.cryptomator.common.Constants.MASTERKEY_BACKUP_SUFFIX;

public final class BackupRestorer {

	private BackupRestorer() {}

	public static boolean restoreIfPresent(Path vaultPath, String fileName) {
		Path targetFile = vaultPath.resolve(fileName);

		try (Stream<Path> files = Files.list(vaultPath)) {
			return files.filter(file -> isValidBackupFileForState(file.getFileName().toString(), fileName))
					.max((f1, f2) -> {
						try {
							FileTime time1 = Files.getLastModifiedTime(f1);
							FileTime time2 = Files.getLastModifiedTime(f2);
							return time1.compareTo(time2);
						} catch (IOException e) {
							return 0;
						}
					})
					.map(backupFile -> copyBackupFile(backupFile, targetFile))
					.orElse(false);
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean isValidBackupFileForState(String fileName, String vaultState) {
		return fileName.startsWith(vaultState) && fileName.endsWith(MASTERKEY_BACKUP_SUFFIX);
	}

	private static boolean copyBackupFile(Path backupFile, Path configPath) {
		try {
			Files.copy(backupFile, configPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
