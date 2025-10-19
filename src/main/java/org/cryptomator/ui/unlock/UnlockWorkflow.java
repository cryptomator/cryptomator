package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.mount.ConflictingMountServiceException;
import org.cryptomator.common.mount.IllegalMountPointException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultIdentity;
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
import java.net.URI;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.concurrent.TimeUnit;
import org.cryptomator.cryptolib.api.Masterkey;

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
			// For masterkey file strategy, we need to load the key first to detect which identity
			if (keyLoadingStrategy instanceof org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingStrategy masterkeyStrategy) {
				// Load the key first - this will detect which identity (primary or hidden)
				URI keyId = vault.getVaultConfigCache().get().getKeyId();
				Masterkey masterkey = masterkeyStrategy.loadKey(keyId);
				
				try {
					// Now get the detected identity
					VaultIdentity selectedIdentity = masterkeyStrategy.getSelectedIdentity();
					if (selectedIdentity != null) {
						// Create a loader that returns our already-loaded masterkey
						org.cryptomator.cryptolib.api.MasterkeyLoader loader = ignored -> masterkey.copy();
						vault.unlock(loader, selectedIdentity);
					} else {
						// Fallback to primary
						org.cryptomator.cryptolib.api.MasterkeyLoader loader = ignored -> masterkey.copy();
						vault.unlock(loader);
					}
				} finally {
					// Clean up the masterkey
					masterkey.destroy();
				}
			} else {
				// For other strategies (e.g., Hub), use default behavior
				keyLoadingStrategy.<RuntimeException>use(keyLoader -> {
					try {
						vault.unlock(keyLoader);
					} catch (IOException | CryptoException | MountFailedException e) {
						throw new WrappedException(e);
					}
				});
			}
			return null;
		} catch (UnlockCancelledException e) {
			cancel(false); // set Tasks state to cancelled
			return null;
		} catch (WrappedException e) {
			Exception cause = e.getCause();
			if (cause instanceof IOException ioe) {
				throw ioe;
			} else if (cause instanceof CryptoException ce) {
				throw ce;
			} else if (cause instanceof MountFailedException mfe) {
				throw mfe;
			}
			throw new IllegalStateException("Unexpected exception type", cause);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Unexpected exception type", e);
		}
	}

	/**
	 * Wrapper for checked exceptions that need to be thrown from lambdas.
	 */
	private static class WrappedException extends RuntimeException {
		WrappedException(Exception cause) {
			super(cause);
		}

		@Override
		public synchronized Exception getCause() {
			return (Exception) super.getCause();
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
		var readOnlyDialog = dialogs.prepareRetryIfReadonlyDialog(mainWindow, stage -> {
			stage.close();
			this.retry();
		}).build();

		Platform.runLater(readOnlyDialog::showAndWait);
	}

	private void retry() {
		try {
			vault.getVaultSettings().usesReadOnlyMode.set(true);
			var isLocked = vault.stateProperty().awaitState(VaultState.Value.LOCKED, 5, TimeUnit.SECONDS);
			if (!isLocked) {
				LOG.error("Vault did not changed to LOCKED state within 5 seconds. Aborting unlock retry.");
			} else {
				appWindows.startUnlockWorkflow(vault, mainWindow);
			}
		} catch (InterruptedException e) {
			LOG.error("Waiting for LOCKED vault state was interrupted. Aborting unlock retry.", e);
			Thread.currentThread().interrupt();
		}
	}

	@Override
	protected void cancelled() {
		LOG.debug("Unlock of '{}' canceled.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

}
