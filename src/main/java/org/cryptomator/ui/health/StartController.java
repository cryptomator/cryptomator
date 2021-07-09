package org.cryptomator.ui.health;

import com.google.common.base.Preconditions;
import dagger.Lazy;
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
import javax.inject.Named;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@HealthCheckScoped
public class StartController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(StartController.class);

	private final Stage window;
	private final Stage unlockWindow;
	private final ObjectProperty<VaultConfig.UnverifiedVaultConfig> unverifiedVaultConfig;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ExecutorService executor;
	private final AtomicReference<Masterkey> masterkeyRef;
	private final AtomicReference<VaultConfig> vaultConfigRef;
	private final Lazy<Scene> checkScene;
	private final Lazy<ErrorComponent.Builder> errorComponent;

	@Inject
	public StartController(@HealthCheckWindow Stage window, HealthCheckComponent.LoadUnverifiedConfigResult configLoadResult, @HealthCheckWindow KeyLoadingStrategy keyLoadingStrategy, ExecutorService executor, AtomicReference<Masterkey> masterkeyRef, AtomicReference<VaultConfig> vaultConfigRef, @FxmlScene(FxmlFile.HEALTH_CHECK_LIST) Lazy<Scene> checkScene, Lazy<ErrorComponent.Builder> errorComponent, @Named("unlockWindow") Stage unlockWindow) {
		Preconditions.checkNotNull(configLoadResult.config());
		this.window = window;
		this.unlockWindow = unlockWindow;
		this.unverifiedVaultConfig = new SimpleObjectProperty<>(configLoadResult.config());
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.executor = executor;
		this.masterkeyRef = masterkeyRef;
		this.vaultConfigRef = vaultConfigRef;
		this.checkScene = checkScene;
		this.errorComponent = errorComponent;
	}

	@FXML
	public void close() {
		LOG.trace("StartController.close()");
		window.close();
	}

	@FXML
	public void next() {
		LOG.trace("StartController.next()");
		CompletableFuture.runAsync(this::loadKey, executor).whenCompleteAsync(this::loadedKey, Platform::runLater);
	}

	private void loadKey() {
		assert !Platform.isFxApplicationThread();
		assert unverifiedVaultConfig.get() != null;
		var unverifiedCfg = unverifiedVaultConfig.get();
		try (var masterkey = keyLoadingStrategy.loadKey(unverifiedCfg.getKeyId())) {
			var verifiedCfg = unverifiedCfg.verify(masterkey.getEncoded(), unverifiedCfg.allegedVaultVersion());
			vaultConfigRef.set(verifiedCfg);
			var old = masterkeyRef.getAndSet(masterkey.clone());
			if (old != null) {
				old.destroy();
			}
		} catch (MasterkeyLoadingFailedException e) {
			if (keyLoadingStrategy.recoverFromException(e)) {
				// retry
				loadKey();
			} else {
				throw new LoadingFailedException(e);
			}
		} catch (VaultConfigLoadException e) {
			throw new LoadingFailedException(e);
		}
	}

	private void loadedKey(Void unused, Throwable exception) {
		assert Platform.isFxApplicationThread();
		if (exception instanceof LoadingFailedException) {
			loadingKeyFailed(exception.getCause());
		} else if (exception != null) {
			loadingKeyFailed(exception);
		} else {
			LOG.debug("Loaded valid key");
			unlockWindow.close();
			window.setScene(checkScene.get());
		}
	}

	private void loadingKeyFailed(Throwable e) {
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

	/* internal types */

	private static class LoadingFailedException extends CompletionException {

		LoadingFailedException(Throwable cause) {
			super(cause);
		}
	}
}
