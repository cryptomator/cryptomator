package org.cryptomator.ui.unlock;

import dagger.Lazy;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume.VolumeException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.unlock.UnlockModule.PasswordEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final AtomicReference<char[]> password;
	private final AtomicBoolean savePassword;
	private final Optional<char[]> savedPassword;
	private final UserInteractionLock<PasswordEntry> passwordEntryLock;
	private final KeychainManager keychain;
	private final Lazy<Scene> unlockScene;
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> invalidMountPointScene;
	private final ErrorComponent.Builder errorComponent;

	@Inject
	UnlockWorkflow(@UnlockWindow Stage window, @UnlockWindow Vault vault, VaultService vaultService, AtomicReference<char[]> password, @Named("savePassword") AtomicBoolean savePassword, @Named("savedPassword") Optional<char[]> savedPassword, UserInteractionLock<PasswordEntry> passwordEntryLock, KeychainManager keychain, @FxmlScene(FxmlFile.UNLOCK) Lazy<Scene> unlockScene, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, @FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT) Lazy<Scene> invalidMountPointScene, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.vault = vault;
		this.vaultService = vaultService;
		this.password = password;
		this.savePassword = savePassword;
		this.savedPassword = savedPassword;
		this.passwordEntryLock = passwordEntryLock;
		this.keychain = keychain;
		this.unlockScene = unlockScene;
		this.successScene = successScene;
		this.invalidMountPointScene = invalidMountPointScene;
		this.errorComponent = errorComponent;

		setOnFailed(event -> {
			Throwable throwable = event.getSource().getException();
			if (throwable instanceof InvalidMountPointException) {
				handleInvalidMountPoint((InvalidMountPointException) throwable);
			} else {
				handleGenericError(throwable);
			}
		});
	}

	@Override
	protected Boolean call() throws InterruptedException, IOException, VolumeException, InvalidMountPointException {
		try {
			if (attemptUnlock()) {
				handleSuccess();
				return true;
			} else {
				cancel(false); // set Tasks state to cancelled
				return false;
			}
		} finally {
			wipePassword(password.get());
			wipePassword(savedPassword.orElse(null));
		}
	}

	private boolean attemptUnlock() throws InterruptedException, IOException, VolumeException, InvalidMountPointException {
		boolean proceed = password.get() != null || askForPassword(false) == PasswordEntry.PASSWORD_ENTERED;
		while (proceed) {
			try {
				vault.unlock(CharBuffer.wrap(password.get()));
				return true;
			} catch (InvalidPassphraseException e) {
				proceed = askForPassword(true) == PasswordEntry.PASSWORD_ENTERED;
			}
		}
		return false;
	}

	private PasswordEntry askForPassword(boolean animateShake) throws InterruptedException {
		Platform.runLater(() -> {
			window.setScene(unlockScene.get());
			window.show();
			Window owner = window.getOwner();
			if (owner != null) {
				window.setX(owner.getX() + (owner.getWidth() - window.getWidth()) / 2);
				window.setY(owner.getY() + (owner.getHeight() - window.getHeight()) / 2);
			} else {
				window.centerOnScreen();
			}
			if (animateShake) {
				Animations.createShakeWindowAnimation(window).play();
			}
		});
		return passwordEntryLock.awaitInteraction();
	}

	private void handleSuccess() {
		LOG.info("Unlock of '{}' succeeded.", vault.getDisplayName());
		if (savePassword.get()) {
			savePasswordToSystemkeychain();
		}
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
	}

	private void savePasswordToSystemkeychain() {
		if (keychain.isSupported()) {
			try {
				keychain.storePassphrase(vault.getId(), CharBuffer.wrap(password.get()));
			} catch (KeychainAccessException e) {
				LOG.error("Failed to store passphrase in system keychain.", e);
			}
		}
	}

	private void handleInvalidMountPoint(InvalidMountPointException impExc) {
		MountPointRequirement requirement = vault.getVolume().orElseThrow(() -> new IllegalStateException("Invalid Mountpoint without a Volume?!", impExc)).getMountPointRequirement();
		assert requirement != MountPointRequirement.NONE; //An invalid MountPoint with no required MountPoint doesn't seem sensible
		assert requirement != MountPointRequirement.PARENT_OPT_MOUNT_POINT; //Not implemented anywhere (yet)

		Throwable cause = impExc.getCause();
		// TODO: apply https://openjdk.java.net/jeps/8213076 in future JDK versions
		if (cause instanceof NotDirectoryException) {
			if (requirement == MountPointRequirement.PARENT_NO_MOUNT_POINT) {
				LOG.error("Unlock failed. Parent folder is missing: {}", cause.getMessage());
			} else {
				LOG.error("Unlock failed. Mountpoint doesn't exist (needs to be a folder): {}", cause.getMessage());
			}
			showInvalidMountPointScene();
			return;
		} else if (cause instanceof FileAlreadyExistsException) {
			LOG.error("Unlock failed. Mountpoint already exists: {}", cause.getMessage());
			showInvalidMountPointScene();
			return;
		} else if (cause instanceof DirectoryNotEmptyException) {
			LOG.error("Unlock failed. Mountpoint not an empty directory: {}", cause.getMessage());
			showInvalidMountPointScene();
			return;
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
		errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
	}

	private void wipePassword(char[] pw) {
		if (pw != null) {
			Arrays.fill(pw, ' ');
		}
	}

	@Override
	protected void scheduled() {
		vault.setState(VaultState.PROCESSING);
	}

	@Override
	protected void succeeded() {
		vault.setState(VaultState.UNLOCKED);
	}

	@Override
	protected void failed() {
		vault.setState(VaultState.LOCKED);
	}

	@Override
	protected void cancelled() {
		vault.setState(VaultState.LOCKED);
	}

}
