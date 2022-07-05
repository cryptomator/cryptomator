package org.cryptomator.ui.quit;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import java.awt.desktop.QuitResponse;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class QuitForcedController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(QuitForcedController.class);

	private final Stage window;
	private final ObservableList<Vault> unlockedVaults;
	private final ExecutorService executorService;
	private final VaultService vaultService;
	private final AtomicReference<QuitResponse> quitResponse;

	/* FXML */
	public Button forceLockAndQuitButton;

	@Inject
	QuitForcedController(@QuitWindow Stage window, ObservableList<Vault> vaults, ExecutorService executorService, VaultService vaultService, @QuitWindow AtomicReference<QuitResponse> quitResponse) {
		this.window = window;
		this.unlockedVaults = vaults.filtered(Vault::isUnlocked);
		this.executorService = executorService;
		this.vaultService = vaultService;
		this.quitResponse = quitResponse;
		window.setOnCloseRequest(windowEvent -> cancel());
	}

	private void respondToQuitRequest(Consumer<QuitResponse> action) {
		var response = quitResponse.getAndSet(null);
		if (response != null) {
			action.accept(response);
		}
	}

	@FXML
	public void cancel() {
		LOG.info("Quitting application forced canceled by user.");
		window.close();
		respondToQuitRequest(QuitResponse::cancelQuit);
	}

	@FXML
	public void forceLockAndQuit() {
		forceLockAndQuitButton.setDisable(true);
		forceLockAndQuitButton.setContentDisplay(ContentDisplay.LEFT);

		Task<Collection<Vault>> lockAllTask = vaultService.createLockAllTask(unlockedVaults, true); // forced set to true
		lockAllTask.setOnSucceeded(evt -> {
			LOG.info("Locked {}", lockAllTask.getValue().stream().map(Vault::getDisplayName).collect(Collectors.joining(", ")));
			if (unlockedVaults.isEmpty()) {
				window.close();
				respondToQuitRequest(QuitResponse::performQuit);
			}
		});
		lockAllTask.setOnFailed(evt -> {
			//TODO: what will happen if force lock and quit app fails?

			LOG.error("Forced locking failed", lockAllTask.getException());
			forceLockAndQuitButton.setDisable(false);
			forceLockAndQuitButton.setContentDisplay(ContentDisplay.TEXT_ONLY);

			window.close();
			respondToQuitRequest(QuitResponse::cancelQuit);
		});
		executorService.execute(lockAllTask);
	}

}
