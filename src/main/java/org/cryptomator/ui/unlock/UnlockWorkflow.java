package org.cryptomator.ui.unlock;

import com.google.common.base.Throwables;
import dagger.Lazy;
import org.cryptomator.common.mount.IllegalMountPointException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A multi-step task that consists of background activities as well as user interaction.
 * <p>
 * This class runs the unlock process and controls when to display which UI.
 */
@UnlockScoped
public class UnlockWorkflow extends Task<Boolean> {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockWorkflow.class);

	private final Stage window;
	private final Vault vault;
	private final VaultService vaultService;
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> invalidMountPointScene;
	private final FxApplicationWindows appWindows;
	private final KeyLoadingStrategy keyLoadingStrategy;
	private final AtomicReference<Throwable> unlockFailedException;

	@Inject
	UnlockWorkflow(@UnlockWindow Stage window, @UnlockWindow Vault vault, VaultService vaultService, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, @FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT) Lazy<Scene> invalidMountPointScene, FxApplicationWindows appWindows, @UnlockWindow KeyLoadingStrategy keyLoadingStrategy, @UnlockWindow AtomicReference<Throwable> unlockFailedException) {
		this.window = window;
		this.vault = vault;
		this.vaultService = vaultService;
		this.successScene = successScene;
		this.invalidMountPointScene = invalidMountPointScene;
		this.appWindows = appWindows;
		this.keyLoadingStrategy = keyLoadingStrategy;
		this.unlockFailedException = unlockFailedException;
	}

	@Override
	protected Boolean call() throws InterruptedException, IOException, CryptoException, MountFailedException {
		try {
			attemptUnlock();
			return true;
		} catch (UnlockCancelledException e) {
			cancel(false); // set Tasks state to cancelled
			return false;
		}
	}

	private void attemptUnlock() throws IOException, CryptoException, MountFailedException {
		try {
			keyLoadingStrategy.use(vault::unlock);
		} catch (Exception e) {
			Throwables.propagateIfPossible(e, IOException.class);
			Throwables.propagateIfPossible(e, CryptoException.class);
			Throwables.propagateIfPossible(e, IllegalMountPointException.class);
			Throwables.propagateIfPossible(e, MountFailedException.class);
			throw new IllegalStateException("unexpected exception type", e);
		}
	}

	private void handleIllegalMountPointError(IllegalMountPointException impe) {
		Platform.runLater(() -> {
			unlockFailedException.set(impe);
			window.setScene(invalidMountPointScene.get());
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
		if(throwable instanceof IllegalMountPointException impe) {
			handleIllegalMountPointError(impe);
		} else {
			handleGenericError(throwable);
		}
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

	@Override
	protected void cancelled() {
		LOG.debug("Unlock of '{}' canceled.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

}
