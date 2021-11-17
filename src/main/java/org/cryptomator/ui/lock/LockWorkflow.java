package org.cryptomator.ui.lock;

import dagger.Lazy;
import org.cryptomator.common.vaults.LockNotCompletedException;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.ErrorComponent;
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
 * This class implements the Task interface, sucht that it can run in the background with some possible foreground operations/requests to the ui, without blocking the main app.
 * If the task state is
 * <li>succeeded, the vault was successfully locked;</li>
 * <li>canceled, the lock was canceled;</li>
 * <li>failed, the lock failed due to an exception.</li>
 */
public class LockWorkflow extends Task<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(LockWorkflow.class);

	private final Stage lockWindow;
	private final Vault vault;
	private final UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock;
	private final Lazy<Scene> lockForcedScene;
	private final Lazy<Scene> lockFailedScene;
	private final ErrorComponent.Builder errorComponent;

	@Inject
	public LockWorkflow(@LockWindow Stage lockWindow, @LockWindow Vault vault, UserInteractionLock<LockModule.ForceLockDecision> forceLockDecisionLock, @FxmlScene(FxmlFile.LOCK_FORCED) Lazy<Scene> lockForcedScene, @FxmlScene(FxmlFile.LOCK_FAILED) Lazy<Scene> lockFailedScene, ErrorComponent.Builder errorComponent) {
		this.lockWindow = lockWindow;
		this.vault = vault;
		this.forceLockDecisionLock = forceLockDecisionLock;
		this.lockForcedScene = lockForcedScene;
		this.lockFailedScene = lockFailedScene;
		this.errorComponent = errorComponent;
	}

	@Override
	protected Void call() throws Volume.VolumeException, InterruptedException, LockNotCompletedException {
		lock(false);
		return null;
	}

	private void lock(boolean forced) throws InterruptedException {
		try {
			vault.lock(forced);
		} catch (Volume.VolumeException | LockNotCompletedException e) {
			LOG.info("Locking {} failed (forced: {}).", vault.getDisplayName(), forced, e);
			var decision = askUserForAction();
			switch (decision) {
				case FORCE -> lock(true);
				case CANCEL -> cancel(false);
			}
		}
	}

	private LockModule.ForceLockDecision askUserForAction() throws InterruptedException {
		forceLockDecisionLock.reset(null);
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
		return forceLockDecisionLock.awaitInteraction();
	}

	@Override
	protected void succeeded() {
		LOG.info("Lock of {} succeeded.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.LOCKED);
	}

	@Override
	protected void failed() {
		final var throwable = super.getException();
		LOG.warn("Lock of {} failed.", vault.getDisplayName(), throwable);
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.UNLOCKED);
		if (throwable instanceof Volume.VolumeException) {
			lockWindow.setScene(lockFailedScene.get());
			lockWindow.show();
		} else {
			errorComponent.cause(throwable).window(lockWindow).build().showErrorScene();
		}
	}

	@Override
	protected void cancelled() {
		LOG.debug("Lock of {} canceled.", vault.getDisplayName());
		vault.stateProperty().transition(VaultState.Value.PROCESSING, VaultState.Value.UNLOCKED);
	}

}
