package org.cryptomator.common.recovery;

import static org.cryptomator.common.vaults.VaultState.Value.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

import org.cryptomator.common.vaults.VaultState.Value;

public final class BackupRestorer {

	private BackupRestorer() {}

	public static boolean restoreIfPresent(Path vaultPath, Value vaultState) {
		Path targetFile;
		switch (vaultState) {
			case VAULT_CONFIG_MISSING -> targetFile = vaultPath.resolve("vault.cryptomator");
			case MASTERKEY_MISSING -> targetFile = vaultPath.resolve("masterkey.cryptomator");
			default -> {
				return false;
			}
		}

		try (Stream<Path> files = Files.list(vaultPath)) {
			return files
					.filter(file -> isValidBackupFileForState(file.getFileName().toString(), vaultState))
					.findFirst()
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
