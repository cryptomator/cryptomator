package org.cryptomator.ui.convertvault;

import com.google.common.base.Preconditions;
import dagger.Lazy;
import org.cryptomator.common.Constants;
import org.cryptomator.common.Passphrase;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultVersionMismatchException;
import org.cryptomator.cryptofs.common.BackupHelper;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.recoverykey.RecoveryKeyFactory;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.cryptomator.common.Constants.MASTERKEY_BACKUP_SUFFIX;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;

public class HubToPasswordConvertController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(HubToPasswordConvertController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final FxApplicationWindows applicationWindows;
	private final Vault vault;
	private final StringProperty recoveryKey;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final ExecutorService backgroundExecutorService;
	private final ResourceBundle resourceBundle;
	private final BooleanProperty conversionStarted;

	@FXML
	NewPasswordController newPasswordController;
	public Button convertBtn;

	@Inject
	public HubToPasswordConvertController(@ConvertVaultWindow Stage window, @FxmlScene(FxmlFile.CONVERTVAULT_HUBTOPASSWORD_SUCCESS) Lazy<Scene> successScene, FxApplicationWindows applicationWindows, @ConvertVaultWindow Vault vault, @ConvertVaultWindow StringProperty recoveryKey, RecoveryKeyFactory recoveryKeyFactory, MasterkeyFileAccess masterkeyFileAccess, ExecutorService backgroundExecutorService, ResourceBundle resourceBundle) {
		this.window = window;
		this.successScene = successScene;
		this.applicationWindows = applicationWindows;
		this.vault = vault;
		this.recoveryKey = recoveryKey;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.backgroundExecutorService = backgroundExecutorService;
		this.resourceBundle = resourceBundle;
		this.conversionStarted = new SimpleBooleanProperty(false);

	}

	@FXML
	public void initialize() {
		convertBtn.disableProperty().bind(Bindings.createBooleanBinding( //
				() -> !newPasswordController.isGoodPassword() || conversionStarted.get(), //
				newPasswordController.goodPasswordProperty(), //
				conversionStarted));
		convertBtn.contentDisplayProperty().bind(Bindings.createObjectBinding( //
				() -> conversionStarted.getValue() ? ContentDisplay.LEFT : ContentDisplay.TEXT_ONLY, //
				conversionStarted));
		convertBtn.textProperty().bind(Bindings.createStringBinding( //
				() -> resourceBundle.getString("convertVault.convert.convertBtn." + (conversionStarted.get() ? "processing" : "before")), //
				conversionStarted));
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void convert() {
		Preconditions.checkState(newPasswordController.isGoodPassword());
		LOG.info("Converting access method of vault {} from hub to password", vault.getPath());
		CompletableFuture.runAsync(() -> conversionStarted.setValue(true), Platform::runLater) //
				.thenRunAsync(this::convertInternal, backgroundExecutorService) //
				.whenCompleteAsync((result, exception) -> {
					if (exception == null) {
						LOG.info("Conversion of vault {} succeeded.", vault.getPath());
						window.setScene(successScene.get());
					} else {
						LOG.error("Conversion of vault {} failed.", vault.getPath(), exception);
						applicationWindows.showErrorWindow(exception, window, null);
					}
				}, Platform::runLater); //
	}

	@VisibleForTesting
	void convertInternal() throws CompletionException, IllegalArgumentException {
		var passphrase = newPasswordController.getNewPassword();
		var vaultPath = vault.getPath();
		try {
			//create masterkey
			recoveryKeyFactory.newMasterkeyFileWithPassphrase(vaultPath, recoveryKey.get(), passphrase);
			LOG.debug("Successfully created masterkey file for vault {}", vaultPath);
			//create password config
			Path passwordConfigPath = vaultPath.resolve("passwordBased." + VAULTCONFIG_FILENAME + ".tmp");
			passwordConfigPath = createPasswordConfig(passwordConfigPath, vaultPath.resolve(MASTERKEY_FILENAME), passphrase);
			//backup hub config
			var hubConfigPath = vaultPath.resolve(VAULTCONFIG_FILENAME);
			backupHubConfig(hubConfigPath);
			//replace hub by password
			Files.move(passwordConfigPath, hubConfigPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (MasterkeyLoadingFailedException e) {
			throw new CompletionException(new IOException("Vault conversion failed", e));
		} catch (IOException e) {
			throw new CompletionException("Vault conversion failed", e);
		} finally {
			passphrase.destroy();
		}
	}

	@VisibleForTesting
	void backupHubConfig(Path hubConfigPath) throws IOException {
		byte[] hubConfigBytes = Files.readAllBytes(hubConfigPath);
		Path backupPath = hubConfigPath.resolveSibling(VAULTCONFIG_FILENAME + BackupHelper.generateFileIdSuffix(hubConfigBytes) + MASTERKEY_BACKUP_SUFFIX);
		Files.copy(hubConfigPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
		LOG.debug("Successfully created hub config backup {}", backupPath.getFileName());
	}

	@VisibleForTesting
	Path createPasswordConfig(Path passwordConfigPath, Path masterkeyFile, Passphrase passphrase) throws IOException, MasterkeyLoadingFailedException {
		var unverifiedVaultConfig = vault.getVaultConfigCache().get();
		try (var masterkey = masterkeyFileAccess.load(masterkeyFile, passphrase)) {
			var hubConfig = unverifiedVaultConfig.verify(masterkey.getEncoded(), unverifiedVaultConfig.allegedVaultVersion());
			var passwordConfig = VaultConfig.createNew() //
					.cipherCombo(hubConfig.getCipherCombo()) //
					.shorteningThreshold(hubConfig.getShorteningThreshold()) //
					.build();
			if (passwordConfig.getVaultVersion() != hubConfig.getVaultVersion()) {
				throw new VaultVersionMismatchException("Only vaults of version " + passwordConfig.getVaultVersion() + " can be converted.");
			}
			var token = passwordConfig.toToken(Constants.DEFAULT_KEY_ID.toString(), masterkey.getEncoded());
			Files.writeString(passwordConfigPath, token, StandardCharsets.US_ASCII, WRITE, CREATE_NEW);
			LOG.debug("Successfully created password config {}", passwordConfigPath);
			return passwordConfigPath;
		}
	}

}
