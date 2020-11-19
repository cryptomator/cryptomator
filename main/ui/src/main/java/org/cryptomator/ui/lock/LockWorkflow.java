package org.cryptomator.ui.lock;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * The sequence of actions performed and checked during lock of a vault.
 * <p>
 * This class implements the Task interface, sucht that it can run in the background with some possible forground operations/requests to the ui, without blocking the main app.
 * If the task succeeded, the vault was successfully locked.
 * If the task is canceled, the lock was canceled.
 * If the task failed, the lock failed due to an exception.
 */
public class LockWorkflow extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(LockWorkflow.class);

	private final Stage lockWindow;
	private final Vault vault;
	private final UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock;
	private final Lazy<Scene> lockForcedScene;
	private final Lazy<Scene> lockFailedScene;

	@Inject
	public LockWorkflow(@LockWindow Stage lockWindow, @LockWindow Vault vault, UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock, @FxmlScene(FxmlFile.LOCK_FORCED) Lazy<Scene> lockForcedScene, @FxmlScene(FxmlFile.LOCK_FAILED) Lazy<Scene> lockFailedScene) {
		this.lockWindow = lockWindow;
		this.vault = vault;
		this.forceLockDecisionLock = forceLockDecisionLock;
		this.lockForcedScene = lockForcedScene;
		this.lockFailedScene = lockFailedScene;
	}

	@Override
	protected Void call() throws Volume.VolumeException, InterruptedException {
		if (!attemptLock()) {
			attemptForcedLock();
		}
		return null;
	}

	private boolean attemptLock() {
		try {
			vault.lock(false);
			return true;
		} catch (Volume.VolumeException e) {
			LOG.debug("Regular lock of {} failed.", vault.getDisplayName(), e);
			return false;
		}
	}

	private boolean attemptForcedLock() throws Volume.VolumeException, InterruptedException {
		// show forcedLock dialogue ...
		Platform.runLater(() -> {
			lockWindow.setScene(lockForcedScene.get());
			lockWindow.show();
			Window owner = lockWindow.getOwner();
			if (owner != null) {
				lockWindow.setX(owner.getX() + (owner.getWidth() - lockWindow.getWidth()) / 2);
				lockWindow.setY(owner.getY() + (owner.getHeight() - lockWindow.getHeight()) / 2);
			} else {
				lockWindow.centerOnScreen();
			}
		});
		// ... and wait for answer
		switch (forceLockDecisionLock.awaitInteraction()) {
			case FORCE:
				vault.lock(true);
				return true;
			case CANCEL:
				this.cancel(false);
				return false;
			default:
				return false;
		}
	}

	@Override
	protected void scheduled() {
		vault.setState(VaultState.PROCESSING);
	}

	@Override
	protected void succeeded() {
		LOG.info("Lock of {} succeeded.", vault.getDisplayName());
		vault.setState(VaultState.LOCKED);
	}

	@Override
	protected void failed() {
		LOG.warn("Failed to lock {}.", vault.getDisplayName());
		vault.setState(VaultState.UNLOCKED);
	}

	@Override
	protected void cancelled() {
		LOG.debug("Lock of {} canceled.", vault.getDisplayName());
		vault.setState(VaultState.UNLOCKED);
	}

}
