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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class StartController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(StartController.class);

	private final Vault vault;
	private final Stage window;
	private final CompletableFuture<VaultConfig.UnverifiedVaultConfig> unverifiedVaultConfig;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ExecutorService executor;
	private final AtomicReference<Masterkey> masterkeyRef;
	private final AtomicReference<VaultConfig> vaultConfigRef;
	private final Lazy<Scene> checkScene;
	private final Lazy<ErrorComponent.Builder> errorComponent;
	private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.LOADING);
	private final BooleanBinding loading = state.isEqualTo(State.LOADING);
	private final BooleanBinding failed = state.isEqualTo(State.FAILED);
	private final BooleanBinding loaded = state.isEqualTo(State.LOADED);

	public enum State {
		LOADING,
		FAILED,
		LOADED
	}

	/* FXML */

	@Inject
	public StartController(@HealthCheckWindow Vault vault, @HealthCheckWindow Stage window, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, @FxmlScene(FxmlFile.HEALTH_CHECK_LIST) Lazy<Scene> checkScene, Lazy<ErrorComponent.Builder> errorComponent) {
		this.vault = vault;
		this.window = window;
		this.unverifiedVaultConfig = CompletableFuture.supplyAsync(this::loadConfig, executor);
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
		this.masterkeyRef = masterkeyRef;
		this.vaultConfigRef = vaultConfigRef;
		this.checkScene = checkScene;
		this.errorComponent = errorComponent;
		this.unverifiedVaultConfig.whenCompleteAsync(this::loadedConfig, Platform::runLater);
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

	private VaultConfig.UnverifiedVaultConfig loadConfig() {
		try {
			return this.vault.getUnverifiedVaultConfig();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void loadedConfig(VaultConfig.UnverifiedVaultConfig cfg, Throwable exception) {
		assert Platform.isFxApplicationThread();
		if (exception != null) {
			state.set(State.FAILED);
		} else {
			assert cfg != null;
			state.set(State.LOADED);
		}
	}

	private void loadKey() {
		assert !Platform.isFxApplicationThread();
		assert unverifiedVaultConfig.isDone();
		var unverifiedCfg = unverifiedVaultConfig.join();
		try (var masterkey = keyLoadingStrategy.loadKey(unverifiedCfg.getKeyId())) {
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

	/* Getter */

	public BooleanBinding loadingProperty() {
		return loading;
	}

	public boolean isLoading() {
		return loading.get();
	}

	public BooleanBinding failedProperty() {
		return failed;
	}

	public boolean isFailed() {
		return failed.get();
	}

	public BooleanBinding loadedProperty() {
		return loaded;
	}

	public boolean isLoaded() {
		return loaded.get();
	}
}
