package org.cryptomator.ui.unlock;

import com.google.common.base.Throwables;
import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume.VolumeException;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NotDirectoryException;

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
	private final ErrorComponent.Builder errorComponent;
	private final KeyLoadingStrategy keyLoadingStrategy;

	@Inject
	UnlockWorkflow(@UnlockWindow Stage window, @UnlockWindow Vault vault, VaultService vaultService, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, @FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT) Lazy<Scene> invalidMountPointScene, ErrorComponent.Builder errorComponent, @UnlockWindow KeyLoadingStrategy keyLoadingStrategy) {
		this.window = window;
		this.vault = vault;
		this.vaultService = vaultService;
		this.successScene = successScene;
		this.invalidMountPointScene = invalidMountPointScene;
		this.errorComponent = errorComponent;
		this.keyLoadingStrategy = keyLoadingStrategy;
	}

	@Override
	protected Boolean call() throws InterruptedException, IOException, VolumeException, InvalidMountPointException, CryptoException {
		try {
			attemptUnlock();
			return true;
		} catch (UnlockCancelledException e) {
			cancel(false); // set Tasks state to cancelled
			return false;
		}
	}

	private void attemptUnlock() throws IOException, VolumeException, InvalidMountPointException, CryptoException {
		try {
			keyLoadingStrategy.use(vault::unlock);
		} catch (Exception e) {
			Throwables.propagateIfPossible(e, IOException.class);
			Throwables.propagateIfPossible(e, VolumeException.class);
			Throwables.propagateIfPossible(e, InvalidMountPointException.class);
			Throwables.propagateIfPossible(e, CryptoException.class);
			throw new IllegalStateException("unexpected exception type", e);
		}
	}

	private void handleInvalidMountPoint(InvalidMountPointException impExc) {
		var requirement = vault.getVolume().orElseThrow(() -> new IllegalStateException("Invalid Mountpoint without a Volume?!", impExc)).getMountPointRequirement();
		assert requirement != MountPointRequirement.NONE; //An invalid MountPoint with no required MountPoint doesn't seem sensible
		assert requirement != MountPointRequirement.PARENT_OPT_MOUNT_POINT; //Not implemented anywhere (yet)
		assert requirement != MountPointRequirement.UNUSED_ROOT_DIR || SystemUtils.IS_OS_WINDOWS; //Not implemented anywhere, but on Windows

		Throwable cause = impExc.getCause();
		// TODO: apply https://openjdk.java.net/jeps/8213076 in future JDK versions
		if (cause instanceof NotDirectoryException) {
			if (requirement == MountPointRequirement.PARENT_NO_MOUNT_POINT) {
				LOG.error("Unlock failed. Parent folder is missing: {}", cause.getMessage());
			} else {
				LOG.error("Unlock failed. Mountpoint doesn't exist (needs to be a folder): {}", cause.getMessage());
			}
			showInvalidMountPointScene();
		} else if (cause instanceof FileAlreadyExistsException) {
			if (requirement == MountPointRequirement.UNUSED_ROOT_DIR) {
				LOG.error("Unlock failed. Drive Letter already in use: {}", cause.getMessage());
			} else {
				LOG.error("Unlock failed. Mountpoint already exists: {}", cause.getMessage());
			}
			showInvalidMountPointScene();
		} else if (cause instanceof DirectoryNotEmptyException) {
			LOG.error("Unlock failed. Mountpoint not an empty directory: {}", cause.getMessage());
			showInvalidMountPointScene();
		} else {
			handleGenericError(impExc);
		}
	}

	private void showInvalidMountPointScene() {
		Platform.runLater(() -> {
			window.setScene(invalidMountPointScene.get());
			window.show();
		});
	}

	private void handleGenericError(Throwable e) {
		LOG.error("Unlock failed for technical reasons.", e);
		errorComponent.cause(e).window(window).build().showErrorScene();
	}

	@Override
	protected void succeeded() {
		LOG.info("Unlock of '{}' succeeded.", vault.getDisplayName());

		switch (vault.getVaultSettings().actionAfterUnlock().get()) {
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
		if (throwable instanceof InvalidMountPointException e) {
			handleInvalidMountPoint(e);
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
