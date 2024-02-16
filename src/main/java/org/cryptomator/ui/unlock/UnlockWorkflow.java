package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.mount.ConflictingMountServiceException;
import org.cryptomator.common.mount.IllegalMountPointException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.fxapp.PrimaryStage;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;

/**
 * A multi-step task that consists of background activities as well as user interaction.
 * <p>
 * This class runs the unlock process and controls when to display which UI.
 */
@UnlockScoped
public class UnlockWorkflow extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockWorkflow.class);

	private final Stage mainWindow;
	private final Stage window;
	private final Vault vault;
	private final VaultService vaultService;
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> invalidMountPointScene;
	private final Lazy<Scene> restartRequiredScene;
	private final FxApplicationWindows appWindows;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final ObjectProperty<IllegalMountPointException> illegalMountPointException;

	@Inject
	UnlockWorkflow(@PrimaryStage Stage mainWindow, //
				   @UnlockWindow Stage window, //
				   @UnlockWindow Vault vault, //
				   VaultService vaultService, //
				   @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, //
				   @FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT) Lazy<Scene> invalidMountPointScene, //
				   @FxmlScene(FxmlFile.UNLOCK_REQUIRES_RESTART) Lazy<Scene> restartRequiredScene, //
				   FxApplicationWindows appWindows, //
				   @UnlockWindow KeyLoadingStrategy keyLoadingStrategy, //
				   @UnlockWindow ObjectProperty<IllegalMountPointException> illegalMountPointException) {
		this.mainWindow = mainWindow;
		this.window = window;
		this.vault = vault;
		this.vaultService = vaultService;
		this.successScene = successScene;
		this.invalidMountPointScene = invalidMountPointScene;
		this.restartRequiredScene = restartRequiredScene;
		this.appWindows = appWindows;
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.illegalMountPointException = illegalMountPointException;
	}

	@Override
	protected Void call() throws InterruptedException, IOException, CryptoException, MountFailedException {
		try {
			keyLoadingStrategy.use(vault::unlock);
			return null;
		} catch (UnlockCancelledException e) {
			cancel(false); // set Tasks state to cancelled
			return null;
		} catch (IOException | RuntimeException | MountFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected exception type", e);
		}
	}

	private void handleIllegalMountPointError(IllegalMountPointException impe) {
		Platform.runLater(() -> {
			illegalMountPointException.set(impe);
			window.setScene(invalidMountPointScene.get());
			window.show();
		});
	}

	private void handleConflictingMountServiceException() {
		Platform.runLater(() -> {
			window.setScene(restartRequiredScene.get());
			window.show();
		});
	}

	private void handleGenericError(Throwable e) {
		LOG.error("Unlock failed for technical reasons.", e);
		appWindows.showErrorWindow(e, window, null);
	}

	private void centerOnPrimaryStage(Window window){
		double centerXPosition = mainWindow.getX() + (mainWindow.getWidth() - window.getWidth()) / 2;
		double centerYPosition = mainWindow.getY() + (mainWindow.getHeight() - window.getHeight()) / 2;
		window.setX(centerXPosition);
		window.setY(centerYPosition);
	}

	@Override
	protected void succeeded() {
		LOG.info("Unlock of '{}' succeeded.", vault.getDisplayName());

		switch (vault.getVaultSettings().actionAfterUnlock.get()) {
			case ASK -> Platform.runLater(() -> {
				window.setScene(successScene.get());
				window.show();
				centerOnPrimaryStage(window);
			});
			case REVEAL -> {
				Platform.runLater(window::close);
				vaultService.reveal(vault);
			}
			case IGNORE -> Platform.runLater(window::close);
		}

		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.UNLOCKED);
	}

	@Override
	protected void failed() {
		LOG.info("Unlock of '{}' failed.", vault.getDisplayName());
		Throwable throwable = super.getException();
		switch (throwable) {
			case IllegalMountPointException e -> handleIllegalMountPointError(e);
			case ConflictingMountServiceException _ -> handleConflictingMountServiceException();
			default -> handleGenericError(throwable);
		}
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

	@Override
	protected void cancelled() {
		LOG.debug("Unlock of '{}' canceled.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

}
