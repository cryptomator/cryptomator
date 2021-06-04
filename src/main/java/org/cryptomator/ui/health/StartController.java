package org.cryptomator.ui.health;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.unlock.UnlockCancelledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class StartController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(StartController.class);

	private final Stage window;
	private final Optional<VaultConfig.UnverifiedVaultConfig> unverifiedVaultConfig;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ExecutorService executor;
	private final AtomicReference<Masterkey> masterkeyRef;
	private final AtomicReference<VaultConfig> vaultConfigRef;
	private final Lazy<Scene> checkScene;
	private final Lazy<ErrorComponent.Builder> errorComponent;

	/* FXML */

	@Inject
	public StartController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, @FxmlScene(FxmlFile.HEALTH_CHECK_LIST) Lazy<Scene> checkScene, Lazy<ErrorComponent.Builder> errorComponent) {
		this.window = window;
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
		this.masterkeyRef = masterkeyRef;
		this.vaultConfigRef = vaultConfigRef;
		this.checkScene = checkScene;
		this.errorComponent = errorComponent;

		//TODO: this is ugly
		//idea: delay the loading of the vault config and show a spinner (something like "check/load config") and react to the result of the loading
		//or: load vault config in a previous step to see if it is loadable.
		VaultConfig.UnverifiedVaultConfig tmp;
		try {
			tmp = vault.getUnverifiedVaultConfig();
		} catch (IOException e) {
			e.printStackTrace();
			tmp =  null;
		}
		this.unverifiedVaultConfig = Optional.ofNullable(tmp);
	}

	@FXML
	public void close() {
		LOG.trace("StartController.close()");
		window.close();
	}

	@FXML
	public void next() {
		LOG.trace("StartController.next()");
		executor.submit(this::loadKey);
	}

	private void loadKey() {
		assert !Platform.isFxApplicationThread();
		assert unverifiedVaultConfig.isPresent();
		try (var masterkey = keyLoadingStrategy.loadKey(unverifiedVaultConfig.orElseThrow().getKeyId())) {
			var unverifiedCfg = unverifiedVaultConfig.get();
			var verifiedCfg = unverifiedCfg.verify(masterkey.getEncoded(), unverifiedCfg.allegedVaultVersion());
			vaultConfigRef.set(verifiedCfg);
			var old = masterkeyRef.getAndSet(masterkey.clone());
			if (old != null) {
				old.destroy();
			}
			Platform.runLater(this::loadedKey);
		} catch (MasterkeyLoadingFailedException e) {
			if (keyLoadingStrategy.recoverFromException(e)) {
				// retry
				loadKey();
			} else {
				Platform.runLater(() -> loadingKeyFailed(e));
			}
		} catch (VaultKeyInvalidException e) {
			Platform.runLater(() -> loadingKeyFailed(e));
		} catch (VaultConfigLoadException e) {
			Platform.runLater(() -> loadingKeyFailed(e));
		}
	}

	private void loadedKey() {
		LOG.debug("Loaded valid key");
		window.setScene(checkScene.get());
	}

	private void loadingKeyFailed(Exception e) {
		if (e instanceof UnlockCancelledException) {
			// ok
		} else if (e instanceof VaultKeyInvalidException) {
			LOG.error("Invalid key"); //TODO: specific error screen
			errorComponent.get().window(window).cause(e).build().showErrorScene();
		} else {
			LOG.error("Failed to load key.", e);
			errorComponent.get().window(window).cause(e).build().showErrorScene();
		}
	}

	public boolean isInvalidConfig() {
		return unverifiedVaultConfig.isEmpty();
	}

}
