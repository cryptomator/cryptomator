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

@QuitScoped
public class QuitController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(QuitController.class);

	private final Stage window;
	private final ObservableList<Vault> unlockedVaults;
	private final ExecutorService executorService;
	private final VaultService vaultService;
	private final AtomicReference<QuitResponse> quitResponse = new AtomicReference<>();
	public Button lockAndQuitButton;

	@Inject
	QuitController(@QuitWindow Stage window, ObservableList<Vault> vaults, ExecutorService executorService, VaultService vaultService) {
		this.window = window;
		this.unlockedVaults = vaults.filtered(Vault::isUnlocked);
		this.executorService = executorService;
		this.vaultService = vaultService;
		window.setOnCloseRequest(windowEvent -> cancel());
	}

	public void updateQuitRequest(QuitResponse newResponse) {
		var oldResponse = quitResponse.getAndSet(newResponse);
		if (oldResponse != null) {
			oldResponse.cancelQuit();
		}
	}

	private void respondToQuitRequest(Consumer<QuitResponse> action) {
		var response = quitResponse.getAndSet(null);
		if (response != null) {
			action.accept(response);
		}
	}

	@FXML
	public void cancel() {
		LOG.info("Quitting application canceled by user.");
		window.close();
		respondToQuitRequest(QuitResponse::cancelQuit);
	}

	@FXML
	public void lockAndQuit() {
		lockAndQuitButton.setDisable(true);
		lockAndQuitButton.setContentDisplay(ContentDisplay.LEFT);

		Task<Collection<Vault>> lockAllTask = vaultService.createLockAllTask(unlockedVaults, false);
		lockAllTask.setOnSucceeded(evt -> {
			LOG.info("Locked {}", lockAllTask.getValue().stream().map(Vault::getDisplayName).collect(Collectors.joining(", ")));
			if (unlockedVaults.isEmpty()) {
				window.close();
				respondToQuitRequest(QuitResponse::performQuit);
			}
		});
		lockAllTask.setOnFailed(evt -> {
			LOG.warn("Locking failed", lockAllTask.getException());
			lockAndQuitButton.setDisable(false);
			lockAndQuitButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
			// TODO: show force lock or force quit scene (and DO NOT cancelQuit() here!) (see https://github.com/cryptomator/cryptomator/pull/1416)
			window.close();
			respondToQuitRequest(QuitResponse::cancelQuit);
		});
		executorService.execute(lockAllTask);
	}

}
