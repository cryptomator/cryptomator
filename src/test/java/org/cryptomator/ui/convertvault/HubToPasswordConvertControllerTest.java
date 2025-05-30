package org.cryptomator.ui.convertvault;

import dagger.Lazy;
import org.cryptomator.common.Constants;
import org.cryptomator.common.Passphrase;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultConfigCache;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

public class HubToPasswordConvertControllerTest {

	@TempDir
	Path tmpDir;

	Stage window;
	Vault vault;
	StringProperty recoveryKey;
	RecoveryKeyFactory recoveryKeyFactory;
	MasterkeyFileAccess masterkeyFileAccess;
	ExecutorService backgroundExecutorService;
	ResourceBundle resourceBundle;
	BooleanProperty isConverting;
	FxApplicationWindows appWindows;
	Lazy<Scene> successScene;

	NewPasswordController newPasswordController;

	HubToPasswordConvertController inTest;

	@BeforeEach
	public void beforeEach() {
		window = Mockito.mock(Stage.class);
		vault = Mockito.mock(Vault.class);
		recoveryKey = Mockito.mock(StringProperty.class);
		recoveryKeyFactory = Mockito.mock(RecoveryKeyFactory.class);
		masterkeyFileAccess = Mockito.mock(MasterkeyFileAccess.class);
		backgroundExecutorService = Mockito.mock(ExecutorService.class);
		resourceBundle = Mockito.mock(ResourceBundle.class);
		isConverting = Mockito.mock(BooleanProperty.class);
		appWindows = Mockito.mock(FxApplicationWindows.class);
		successScene = Mockito.mock(Lazy.class);
		newPasswordController = Mockito.mock(NewPasswordController.class);
		inTest = new HubToPasswordConvertController(window, successScene, appWindows, vault, recoveryKey, recoveryKeyFactory, masterkeyFileAccess, backgroundExecutorService, resourceBundle);
		inTest.newPasswordController = newPasswordController;
		Mockito.when(vault.getPath()).thenReturn(tmpDir);
	}

	@Test
	public void testBackupHubConfig() throws IOException {
		var configContent = "Hello Config!".getBytes();
		Path configPath = tmpDir.resolve(Constants.VAULTCONFIG_FILENAME);
		Files.write(configPath, configContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

		inTest.backupHubConfig(configPath);
		Optional<Path> result = Files.list(tmpDir).filter(p -> {
			var fileName = p.getFileName().toString();
			return fileName.startsWith(Constants.VAULTCONFIG_FILENAME) && fileName.endsWith(Constants.MASTERKEY_BACKUP_SUFFIX);
		}).findAny();

		Assertions.assertTrue(Files.exists(configPath));
		Assertions.assertTrue(result.isPresent());
		Assertions.assertArrayEquals(configContent, Files.readAllBytes(result.get()));
	}


	@Test
	@DisplayName("createPasswordConfig creates valid config with password key id")
	public void integrationTestCreatePasswordConfig(@TempDir Path tmpDir) throws NoSuchAlgorithmException, IOException {
		//prepare
		var csprng = SecureRandom.getInstanceStrong();
		var key = Masterkey.generate(csprng);
		var masterkeyPath = tmpDir.resolve("masterkey");
		MasterkeyFileAccess mkAccess = new MasterkeyFileAccess(Constants.PEPPER, csprng);
		mkAccess.persist(key, masterkeyPath, "");

		var config = VaultConfig.createNew().cipherCombo(CryptorProvider.Scheme.SIV_GCM).shorteningThreshold(42).build();
		var token = config.toToken("test", key.getEncoded());
		var hubConfig = VaultConfig.decode(token);
		var configCache = Mockito.mock(VaultConfigCache.class);
		Mockito.when(vault.getVaultConfigCache()).thenReturn(configCache);
		Mockito.when(configCache.get()).thenReturn(hubConfig);

		var passwordConfigPath = tmpDir.resolve("passwordConfig");

		inTest = new HubToPasswordConvertController(window, successScene, appWindows, vault, recoveryKey, recoveryKeyFactory, mkAccess, backgroundExecutorService, resourceBundle);

		//execute
		Path result = inTest.createPasswordConfig(passwordConfigPath, masterkeyPath, Passphrase.copyOf(""));

		//check
		AtomicReference<VaultConfig.UnverifiedVaultConfig> unverifiedCfg = new AtomicReference<>();
		AtomicReference<VaultConfig> cfg = new AtomicReference<>();
		Assertions.assertTrue(Files.exists(result));
		Assertions.assertDoesNotThrow(() -> {
			var tmp = VaultConfig.decode(Files.readString(result, StandardCharsets.US_ASCII));
			unverifiedCfg.set(tmp);
		});
		Assertions.assertDoesNotThrow(() -> {
			var tmp = unverifiedCfg.get().verify(key.getEncoded(), config.getVaultVersion());
			cfg.set(tmp);
		});
		Assertions.assertAll( //
				() -> Assertions.assertEquals(config.getCipherCombo(), cfg.get().getCipherCombo()), //
				() -> Assertions.assertEquals(config.getVaultVersion(), cfg.get().getVaultVersion()), //
				() -> Assertions.assertEquals(config.getShorteningThreshold(), cfg.get().getShorteningThreshold()), //
				() -> Assertions.assertEquals(Constants.DEFAULT_KEY_ID, unverifiedCfg.get().getKeyId()));
	}

	@Nested
	class ConvertInternalTests {


		Passphrase passphrase = Mockito.mock(Passphrase.class);
		String actualRecoveryKey = "recoveryKey";
		HubToPasswordConvertController inSpy;

		@BeforeEach
		public void beforeEach() throws IOException {
			inSpy = Mockito.spy(inTest);
			Mockito.when(newPasswordController.getNewPassword()).thenReturn(passphrase);
			Mockito.when(recoveryKey.get()).thenReturn(actualRecoveryKey);
			Mockito.doNothing().when(recoveryKeyFactory).newMasterkeyFileWithPassphrase(any(), anyString(), any());
			Mockito.doNothing().when(inSpy).backupHubConfig(any());
			Mockito.doNothing().when(passphrase).destroy();
		}


		@Test
		public void testConvertInternal() throws IOException {
			var passwordConfigContent = "Hello Config!".getBytes();
			Path passwordConfigPath = tmpDir.resolve("passwordConfig");
			Path configPath = tmpDir.resolve(Constants.VAULTCONFIG_FILENAME);
			Files.write(passwordConfigPath, passwordConfigContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			Mockito.doReturn(passwordConfigPath).when(inSpy).createPasswordConfig(any(), any(), eq(passphrase));

			inSpy.convertInternal();

			var inOrder = Mockito.inOrder(inSpy, recoveryKeyFactory, passphrase);
			inOrder.verify(recoveryKeyFactory).newMasterkeyFileWithPassphrase(tmpDir, actualRecoveryKey, passphrase);
			inOrder.verify(inSpy).createPasswordConfig(any(), Mockito.any(), eq(passphrase));
			inOrder.verify(inSpy).backupHubConfig(configPath);
			inOrder.verify(passphrase).destroy();
			Assertions.assertArrayEquals(passwordConfigContent, Files.readAllBytes(configPath));
		}

		@Test
		public void testConvertInternalWrapsCryptoException() throws IOException {
			Mockito.doThrow(new MasterkeyLoadingFailedException("yadda")).when(inSpy).createPasswordConfig(any(), any(), any());

			Assertions.assertThrows(CompletionException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}

		@Test
		public void testConvertInternalWrapsIOException() throws IOException {
			Mockito.doReturn(Mockito.mock(Path.class)).when(inSpy).createPasswordConfig(any(), any(), eq(passphrase));
			Mockito.doThrow(new IOException("yudu")).when(inSpy).backupHubConfig(any());

			Assertions.assertThrows(CompletionException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}

		@Test
		public void testConvertInternalNotWrapsIAE() throws IOException {
			Mockito.doThrow(new IllegalArgumentException("yolo")).when(recoveryKeyFactory).newMasterkeyFileWithPassphrase(any(), anyString(), any());

			Assertions.assertThrows(IllegalArgumentException.class, inSpy::convertInternal);

			Mockito.verify(passphrase, times(1)).destroy();
		}
	}


}
