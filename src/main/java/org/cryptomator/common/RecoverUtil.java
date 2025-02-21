package org.cryptomator.common;

import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.stream.Stream;

import static org.cryptomator.cryptofs.common.Constants.DATA_DIR_NAME;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_CTRMAC;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_GCM;

public class RecoverUtil {

	public static CryptorProvider.Scheme detectCipherCombo(byte[] masterkey, Path pathToVault) {
		try (Stream<Path> paths = Files.walk(pathToVault.resolve(DATA_DIR_NAME))) {
			return paths.filter(path -> path.toString().endsWith(".c9r"))
					.findFirst()
					.map(c9rFile -> determineScheme(c9rFile, masterkey))
					.orElseThrow(() -> new IllegalStateException("No .c9r file found."));
		} catch (IOException e) {
			throw new IllegalStateException("Failed to detect cipher combo.", e);
		}
	}

	private static CryptorProvider.Scheme determineScheme(Path c9rFile, byte[] masterkey) {
		try {
			ByteBuffer header = ByteBuffer.wrap(Files.readAllBytes(c9rFile));
			return tryDecrypt(header, new Masterkey(masterkey), SIV_GCM) ? SIV_GCM :
					tryDecrypt(header, new Masterkey(masterkey), SIV_CTRMAC) ? SIV_CTRMAC : null;
		} catch (IOException e) {
			return null;
		}
	}

	private static boolean tryDecrypt(ByteBuffer header, Masterkey masterkey, CryptorProvider.Scheme scheme) {
		try (Cryptor cryptor = CryptorProvider.forScheme(scheme).provide(masterkey, SecureRandom.getInstanceStrong())) {
			cryptor.fileHeaderCryptor().decryptHeader(header.duplicate());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean restoreBackupIfAvailable(Path configPath, VaultState.Value vaultState) {
		try (Stream<Path> files = Files.list(configPath.getParent())) {
			return files
					.filter(file -> matchesBackupFile(file.getFileName().toString(), vaultState))
					.findFirst()
					.map(backupFile -> copyBackupFile(backupFile, configPath))
					.orElse(false);
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean matchesBackupFile(String fileName, VaultState.Value vaultState) {
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
