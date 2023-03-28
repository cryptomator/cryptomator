package org.cryptomator.ui.convertvault;

import org.cryptomator.common.Constants;
import org.cryptomator.common.Passphrase;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

public class HubToLocalConvertControllerTest {

	Stage window;
	Vault vault;
	StringProperty recoveryKey;
	RecoveryKeyFactory recoveryKeyFactory;
	MasterkeyFileAccess masterkeyFileAccess;
	ExecutorService backgroundExecutorService;
	BooleanProperty isConverting;
	NewPasswordController newPasswordController;

	HubToLocalConvertController inTest;

	@BeforeEach
	public void beforeEach() {
		window = Mockito.mock(Stage.class);
		vault = Mockito.mock(Vault.class);
		recoveryKey = Mockito.mock(StringProperty.class);
		recoveryKeyFactory = Mockito.mock(RecoveryKeyFactory.class);
		masterkeyFileAccess = Mockito.mock(MasterkeyFileAccess.class);
		backgroundExecutorService = Mockito.mock(ExecutorService.class);
		isConverting = Mockito.mock(BooleanProperty.class);
		newPasswordController = Mockito.mock(NewPasswordController.class);
		inTest = new HubToLocalConvertController(window, vault, recoveryKey, recoveryKeyFactory, masterkeyFileAccess, backgroundExecutorService);
		inTest.newPasswordController = newPasswordController;
	}

	@Test
	public void testBackupHubConfig(@TempDir Path tmpDir) throws IOException {
		Path configPath = tmpDir.resolve(Constants.VAULTCONFIG_FILENAME);
		Files.writeString(configPath, "hello config", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Mockito.when(vault.getPath()).thenReturn(tmpDir);

		inTest.backupHubConfig(configPath);
		Optional<Path> result = Files.list(tmpDir).filter(p -> {
			var fileName = p.getFileName().toString();
			return fileName.startsWith(Constants.VAULTCONFIG_FILENAME) && fileName.endsWith(Constants.MASTERKEY_BACKUP_SUFFIX);
		}).findAny();

		Assertions.assertTrue(Files.notExists(configPath));
		Assertions.assertTrue(result.isPresent());
		Assertions.assertEquals("hello config", Files.readString(result.get(), StandardCharsets.UTF_8));
	}

	@Nested
	class ConvertInternalTests {

		Passphrase passphrase = Mockito.mock(Passphrase.class);
		Path vaultPath = Mockito.mock(Path.class, "/vault/path");
		Path configPath = Mockito.mock(Path.class, "/vault/path/config");
		String actualRecoveryKey = "recoveryKey";
		HubToLocalConvertController inSpy;

		@BeforeEach
		public void beforeEach() throws IOException {
			inSpy = Mockito.spy(inTest);
			Mockito.when(newPasswordController.getNewPassword()).thenReturn(passphrase);
			Mockito.when(recoveryKey.get()).thenReturn(actualRecoveryKey);
			Mockito.when(vault.getPath()).thenReturn(vaultPath);
			Mockito.when(vaultPath.resolve(anyString())).thenReturn(configPath);
			Mockito.doNothing().when(recoveryKeyFactory).newMasterkeyFileWithPassphrase(any(), anyString(), any());
			Mockito.doNothing().when(inSpy).backupHubConfig(any());
			Mockito.doNothing().when(inSpy).replaceWithLocalConfig(any());
			Mockito.doNothing().when(passphrase).destroy();
		}


		@Test
		public void testConvertInternal() throws IOException {
			inSpy.convertInternal();

			Mockito.verify(recoveryKeyFactory, times(1)).newMasterkeyFileWithPassphrase(vaultPath, actualRecoveryKey, passphrase);
			Mockito.verify(inSpy, times(1)).backupHubConfig(configPath);
			Mockito.verify(inSpy, times(1)).replaceWithLocalConfig(passphrase);
			Mockito.verify(passphrase, times(1)).destroy();
		}

		@Test
		public void testConvertInternalWrapsCryptoException() throws IOException {
			Mockito.doThrow(new MasterkeyLoadingFailedException("yadda")).when(inSpy).replaceWithLocalConfig(any());

			Assertions.assertThrows(CompletionException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}

		@Test
		public void testConvertInternalWrapsIOException() throws IOException {
			Mockito.doThrow(new IOException("yudu")).when(inSpy).backupHubConfig(any());

			Assertions.assertThrows(CompletionException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}

		@Test
		public void testConvertInternalNotWrapsIAE() throws IOException {
			Mockito.doThrow(new IllegalArgumentException("yudu")).when(recoveryKeyFactory).newMasterkeyFileWithPassphrase(any(),anyString(),any());

			Assertions.assertThrows(IllegalArgumentException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}
	}


}
