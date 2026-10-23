package org.cryptomator.common.vaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.application.Platform;
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

	@Test
	void testRedetermineVaultStateUpdatesState(@TempDir Path tmpDir) {
		var state = new VaultState(VaultState.Value.LOCKED);
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.when(vault.getPath()).thenReturn(tmpDir.resolve("missing"));

		try (var _ = Mockito.mockStatic(Platform.class)) {
			VaultListManager.redetermineVaultState(vault);
		}

		assertEquals(VaultState.Value.MISSING, state.getValue());
	}

	@Test
	void testRedetermineVaultStateKeepsUnchangedState(@TempDir Path tmpDir) {
		var state = new VaultState(VaultState.Value.MISSING);
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.when(vault.getPath()).thenReturn(tmpDir.resolve("missing"));

		try (var _ = Mockito.mockStatic(Platform.class)) {
			assertEquals(VaultState.Value.MISSING, VaultListManager.redetermineVaultState(vault));
		}

		assertEquals(VaultState.Value.MISSING, state.getValue());
	}

	@Test
	void testRedetermineVaultStateKeepsStateAdmittedMeanwhile(@TempDir Path tmpDir) {
		var state = new VaultState(VaultState.Value.LOCKED);
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.when(vault.getPath()).thenAnswer(_ -> {
			state.transition(VaultState.Value.LOCKED, VaultState.Value.PROCESSING); // an unlock is admitted during the check
			return tmpDir.resolve("missing");
		});

		try (var _ = Mockito.mockStatic(Platform.class)) {
			VaultListManager.redetermineVaultState(vault);
		}

		assertEquals(VaultState.Value.PROCESSING, state.getValue());
	}

	@Test
	void testRedetermineVaultStateMarksError(@TempDir Path tmpDir) throws IOException {
		var notADirectory = Files.createFile(tmpDir.resolve("file"));
		var state = new VaultState(VaultState.Value.LOCKED);
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.when(vault.getPath()).thenReturn(notADirectory);

		try (var _ = Mockito.mockStatic(Platform.class)) {
			VaultListManager.redetermineVaultState(vault);
		}

		assertEquals(VaultState.Value.ERROR, state.getValue());
	}

	@Test
	void testRedetermineVaultStateKeepsStateAdmittedMeanwhileOnError(@TempDir Path tmpDir) throws IOException {
		var notADirectory = Files.createFile(tmpDir.resolve("file"));
		var state = new VaultState(VaultState.Value.LOCKED);
		var vault = Mockito.mock(Vault.class);
		Mockito.when(vault.stateProperty()).thenReturn(state);
		Mockito.when(vault.getPath()).thenAnswer(_ -> {
			state.transition(VaultState.Value.LOCKED, VaultState.Value.PROCESSING); // an unlock is admitted during the check
			return notADirectory;
		});

		try (var _ = Mockito.mockStatic(Platform.class)) {
			VaultListManager.redetermineVaultState(vault);
		}

		assertEquals(VaultState.Value.PROCESSING, state.getValue());
	}

}
