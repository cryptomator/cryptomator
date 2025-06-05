package org.cryptomator.common.recovery;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import org.cryptomator.common.vaults.VaultState.Value;

public final class BackupRestorer {

	private BackupRestorer() {}

	public static boolean restoreIfPresent(Path vaultPath, Value vaultState) {
		Path targetFile = switch (vaultState) {
			case VAULT_CONFIG_MISSING -> vaultPath.resolve("vault.cryptomator");
			case MASTERKEY_MISSING -> vaultPath.resolve("masterkey.cryptomator");
			default -> throw new IllegalArgumentException("Unexpected vault state: " + vaultState);
		};

		try (Stream<Path> files = Files.list(vaultPath)) {
			return files.filter(file -> isValidBackupFileForState(file.getFileName().toString(), vaultState))
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

	private static boolean isValidBackupFileForState(String fileName, Value vaultState) {
		return switch (vaultState) {
			case VAULT_CONFIG_MISSING -> fileName.startsWith("vault.cryptomator") && fileName.endsWith(".bkup");
			case MASTERKEY_MISSING -> fileName.startsWith("masterkey.cryptomator") && fileName.endsWith(".bkup");
			default -> false;
		};
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
