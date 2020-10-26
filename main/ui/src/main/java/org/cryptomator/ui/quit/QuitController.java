package org.cryptomator.ui.quit;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.desktop.QuitResponse;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@QuitScoped
public class QuitController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(QuitController.class);

	private final Stage window;
	private final QuitResponse response;
	private final ObservableList<Vault> unlockedVaults;
	private final ExecutorService executorService;
	private final VaultService vaultService;
	public Button lockAndQuitButton;

	@Inject
	QuitController(@QuitWindow Stage window, QuitResponse response, ObservableList<Vault> vaults, ExecutorService executorService, VaultService vaultService) {
		this.window = window;
		this.response = response;
		this.unlockedVaults = vaults.filtered(Vault::isUnlocked);
		this.executorService = executorService;
		this.vaultService = vaultService;
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

		Task<Collection<Vault>> lockAllTask = vaultService.createLockAllTask(unlockedVaults, false);
		lockAllTask.setOnSucceeded(evt -> {
			LOG.info("Locked {}", lockAllTask.getValue().stream().map(Vault::getDisplayName).collect(Collectors.joining(", ")));
			if (unlockedVaults.isEmpty()) {
				window.close();
				response.performQuit();
			}
		});
		lockAllTask.setOnFailed(evt -> {
			LOG.warn("Locking failed", lockAllTask.getException());
			lockAndQuitButton.setDisable(false);
			lockAndQuitButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
			// TODO: show force lock or force quit scene (and DO NOT cancelQuit() here!)
			// see https://github.com/cryptomator/cryptomator/blob/1.4.16/main/ui/src/main/java/org/cryptomator/ui/model/Vault.java#L151-L163
			response.cancelQuit();
		});
		executorService.execute(lockAllTask);
	}

}
