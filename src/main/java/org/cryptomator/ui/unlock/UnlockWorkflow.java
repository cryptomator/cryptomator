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
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.fxapp.PrimaryStage;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
	private final Dialogs dialogs;

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
				   @UnlockWindow ObjectProperty<IllegalMountPointException> illegalMountPointException, //
				   Dialogs dialogs) {
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
		this.dialogs = dialogs;
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

	@Override
	protected void succeeded() {
		LOG.info("Unlock of '{}' succeeded.", vault.getDisplayName());

		switch (vault.getVaultSettings().actionAfterUnlock.get()) {
			case ASK -> Platform.runLater(() -> {
				window.setScene(successScene.get());
				window.show();
				double x = mainWindow.getX() + (mainWindow.getWidth() - window.getWidth()) / 2;
				double y = mainWindow.getY() + (mainWindow.getHeight() - window.getHeight()) / 2;
				if(!mainWindow.isShowing()) {
					Screen screen = Screen.getScreensForRectangle(mainWindow.getX(), mainWindow.getY(), mainWindow.getWidth(), mainWindow.getHeight())
							.stream()
							.findFirst()
							.orElse(Screen.getPrimary());
					Rectangle2D bounds = screen.getVisualBounds();
					x = bounds.getMinX() + (bounds.getWidth() - window.getWidth()) / 2;
					y = bounds.getMinY() + (bounds.getHeight() - window.getHeight()) / 2;
				}
				window.setX(x);
				window.setY(y);
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
			case ReadOnlyFileSystemException _ -> handleReadOnlyFileSystem();
			default -> handleGenericError(throwable);
		}
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

	private void handleReadOnlyFileSystem() {
		CompletableFuture.runAsync(() -> {
			try {
				vault.stateProperty().awaitState(VaultState.Value.LOCKED, 5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}).thenRunAsync(() -> {
			dialogs.prepareRetryIfReadonlyDialog(mainWindow, stage -> {
				vault.getVaultSettings().usesReadOnlyMode.set(true);
				appWindows.startUnlockWorkflow(vault, mainWindow);
				stage.close();
			}).build().showAndWait();
		}, Platform::runLater).exceptionally(_ -> {
			LOG.error("Couldn't display retry dialog.");
			return null;
		});
	}

	@Override
	protected void cancelled() {
		LOG.debug("Unlock of '{}' canceled.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

}
