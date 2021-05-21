package org.cryptomator.ui.health;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.VaultConfig;
import org.cryptomator.cryptofs.VaultConfigLoadException;
import org.cryptomator.cryptofs.VaultKeyInvalidException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;
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

	/* FXML */

	@Inject
	public StartController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, @FxmlScene(FxmlFile.HEALTH_CHECK) Lazy<Scene> checkScene) {
		this.window = window;
		this.unverifiedVaultConfig = vault.getUnverifiedVaultConfig(); //TODO: prevent workflow at all, if the vault config is emtpy
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
		this.masterkeyRef = masterkeyRef;
		this.vaultConfigRef = vaultConfigRef;
		this.checkScene = checkScene;
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
			LOG.error("Invalid key");
			// TODO show error screen
		} else {
			LOG.error("Failed to load key.", e);
			// TODO show error screen
		}
	}

	public boolean isInvalidConfig() {
		return unverifiedVaultConfig.isEmpty();
	}

}
