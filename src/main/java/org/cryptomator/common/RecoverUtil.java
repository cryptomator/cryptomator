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
import java.util.Optional;
import java.util.stream.Stream;

import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;
import static org.cryptomator.cryptofs.common.Constants.DATA_DIR_NAME;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_CTRMAC;
import static org.cryptomator.cryptolib.api.CryptorProvider.Scheme.SIV_GCM;

public class RecoverUtil {

	public static CryptorProvider.Scheme detectCipherCombo(byte[] masterkey, Path pathToVault) {
		try {
			Path dDirPath = pathToVault.resolve(DATA_DIR_NAME);

			Optional<Path> firstC9rFile;
			try (Stream<Path> paths = Files.walk(dDirPath)) {
				firstC9rFile = paths.filter(path -> path.toString().endsWith(".c9r")).findFirst();
			}
			if (firstC9rFile.isEmpty()) {
				throw new IllegalStateException("No .c9r file found.");
			}

			Path c9rFile = firstC9rFile.get();
			if (canDecryptFileHeader(c9rFile, new Masterkey(masterkey), SIV_GCM)) {
				return SIV_GCM;
			}
			if (canDecryptFileHeader(c9rFile, new Masterkey(masterkey), SIV_CTRMAC)) {
				return SIV_CTRMAC;
			}

			return null;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to detect cipher combo.", e);
		}
	}

	private static boolean canDecryptFileHeader(Path c9rFile, Masterkey masterkey, CryptorProvider.Scheme scheme) {
		try (Cryptor cryptor = CryptorProvider.forScheme(scheme).provide(masterkey, SecureRandom.getInstanceStrong())) {
			ByteBuffer header = ByteBuffer.wrap(Files.readAllBytes(c9rFile));
			cryptor.fileHeaderCryptor().decryptHeader(header);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static VaultState.Value tryBackUpConfig(Path pathToConfig, VaultState.Value vaultState) {
		try (Stream<Path> files = Files.list(pathToConfig.getParent())) {
			Path backupFile = files.filter(file -> {
				String fileName = file.getFileName().toString();
				return switch (vaultState) {
					case VAULT_CONFIG_MISSING -> fileName.startsWith("vault.cryptomator") && fileName.endsWith(".bkup");
					case MASTERKEY_MISSING -> fileName.startsWith("masterkey.cryptomator") && fileName.endsWith(".bkup");
					default -> false;
				};
			}).findFirst().orElse(null);

			if (backupFile != null) {
				try {
					Files.copy(backupFile, pathToConfig, StandardCopyOption.REPLACE_EXISTING);
					return LOCKED;
				} catch (IOException e) {
					return vaultState;
				}
			} else {
				return vaultState;
			}
		} catch (IOException e) {
			return vaultState;
		}
	}

}
