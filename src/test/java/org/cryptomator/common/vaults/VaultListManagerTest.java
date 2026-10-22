package org.cryptomator.common.vaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;
import static org.cryptomator.cryptofs.common.Constants.DATA_DIR_NAME;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultListManagerTest {

	@Test
	void testAssertIsVaultDirectoryWhenDataDirIsMissing(@TempDir Path tmpDir) {
		NotAVaultDirectoryException e = assertThrows(NotAVaultDirectoryException.class, () -> {
			VaultListManager.assertIsVaultDirectory(tmpDir);
		});

		assertEquals(NotAVaultDirectoryException.Reason.MISSING_DATA_DIR, e.notAVaultReason());
	}

	@Test
	void testAssertIsVaultDirectoryWhenDataDirIsFile(@TempDir Path tmpDir) throws IOException {
		Files.createFile(tmpDir.resolve(DATA_DIR_NAME));

		NotAVaultDirectoryException e = assertThrows(NotAVaultDirectoryException.class, () -> {
			VaultListManager.assertIsVaultDirectory(tmpDir);
		});

		assertEquals(NotAVaultDirectoryException.Reason.DATA_NOT_A_DIRECTORY, e.notAVaultReason());
	}

	@Test
	void testAssertIsVaultDirectoryWhenVaultConfigAndMasterkeyAreMissing(@TempDir Path tmpDir) throws IOException {
		Files.createDirectory(tmpDir.resolve(DATA_DIR_NAME));

		NotAVaultDirectoryException e = assertThrows(NotAVaultDirectoryException.class, () -> {
			VaultListManager.assertIsVaultDirectory(tmpDir);
		});

		assertEquals(NotAVaultDirectoryException.Reason.MISSING_VAULT_CONFIG, e.notAVaultReason());
	}

	@Test
	void testAssertIsVaultDirectoryAcceptsModernVault(@TempDir Path tmpDir) throws IOException {
		Files.createDirectory(tmpDir.resolve(DATA_DIR_NAME));
		Files.createFile(tmpDir.resolve(VAULTCONFIG_FILENAME));

		assertDoesNotThrow(() -> {
			VaultListManager.assertIsVaultDirectory(tmpDir);
		});
	}

	@Test
	void testAssertIsVaultDirectoryAcceptsLegacyVaultCandidate(@TempDir Path tmpDir) throws IOException {
		Files.createDirectory(tmpDir.resolve(DATA_DIR_NAME));
		Files.createFile(tmpDir.resolve(MASTERKEY_FILENAME));

		assertDoesNotThrow(() -> {
			VaultListManager.assertIsVaultDirectory(tmpDir);
		});
	}

}
