package org.cryptomator.ui.quit;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.desktop.QuitResponse;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@QuitScoped
public class QuitController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(QuitController.class);

	private final Stage window;
	private final QuitResponse response;
	private final ObservableList<Vault> unlockedVaults;
	private final ExecutorService executor;
	public Button lockAndQuitButton;

	@Inject
	QuitController(@QuitWindow Stage window, QuitResponse response, ObservableList<Vault> vaults, ExecutorService executor) {
		this.window = window;
		this.response = response;
		this.unlockedVaults = vaults.filtered(Vault::isUnlocked);
		this.executor = executor;
	}

	@FXML
	public void cancel() {
		LOG.info("Quitting application canceled by user.");
		window.close();
		response.cancelQuit();
	}

	@FXML
	public void lockAndQuit() {
		lockAndQuitButton.setDisable(true);
		lockAndQuitButton.setContentDisplay(ContentDisplay.LEFT);
		
		Iterator<Vault> toBeLocked = List.copyOf(unlockedVaults).iterator();
		ScheduledService<Void> lockAllService = new LockAllVaultsService(executor, toBeLocked);
		lockAllService.setOnSucceeded(evt -> {
			if (!toBeLocked.hasNext()) {
				window.close();
				response.performQuit();
			}
		});
		lockAllService.setOnFailed(evt -> {
			lockAndQuitButton.setDisable(false);
			lockAndQuitButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
			// TODO: show force lock or force quit scene (and DO NOT cancelQuit() here!)
			response.cancelQuit();
		});
		lockAllService.start();
	}

	/**
	 * @param vault The vault to lock
	 * @return Task that tries to lock the given vault gracefully.
	 */
	private Task<Void> createGracefulLockTask(Vault vault) {
		Task task = new Task<Void>() {
			@Override
			protected Void call() throws Volume.VolumeException {
				vault.lock(false);
				LOG.info("Locked {}", vault.getDisplayableName());
				return null;
			}
		};
		task.setOnSucceeded(evt -> {
			vault.setState(VaultState.LOCKED);
		});
		task.setOnFailed(evt -> {
			LOG.warn("Failed to lock vault", vault);
		});
		return task;
	}

	/**
	 * @return Task that succeeds immediately
	 */
	private Task<Void> createNoopTask() {
		return new Task<>() {
			@Override
			protected Void call() {
				return null;
			}
		};
	}
	
	private class LockAllVaultsService extends ScheduledService<Void> {

		private final Iterator<Vault> vaultsToLock;

		public LockAllVaultsService(Executor executor, Iterator<Vault> vaultsToLock) {
			this.vaultsToLock = vaultsToLock;
			setExecutor(executor);
			setRestartOnFailure(false);
		}

		@Override
		protected Task<Void> createTask() {
			assert Platform.isFxApplicationThread();
			if (vaultsToLock.hasNext()) {
				return createGracefulLockTask(vaultsToLock.next());
			} else {
				// This should be unreachable code, since vaultsToLock is only accessed on the FX App Thread.
				// But if quitting the application takes longer for any reason, this service should shut down properly
				reset();
				return createNoopTask();
			}
		}
	}
	
}
