package org.cryptomator.ui.unlock;

import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;

@UnlockScoped
public class UnlockSuccessController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockSuccessController.class);

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final VaultService vaultService;
	private final ObjectProperty<ContentDisplay> revealButtonState;
	private final BooleanProperty revealButtonDisabled;

	/* FXML */
	public CheckBox rememberChoiceCheckbox;

	@Inject
	public UnlockSuccessController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor, VaultService vaultService) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.vaultService = vaultService;
		this.revealButtonState = new SimpleObjectProperty<>(ContentDisplay.TEXT_ONLY);
		this.revealButtonDisabled = new SimpleBooleanProperty();
	}

	@FXML
	public void close() {
		LOG.trace("UnlockSuccessController.close()");
		window.close();
		if (rememberChoiceCheckbox.isSelected()) {
			vault.getVaultSettings().actionAfterUnlock.setValue(WhenUnlocked.IGNORE);
		}
	}

	@FXML
	public void revealAndClose() {
		LOG.trace("UnlockSuccessController.revealAndClose()");
		revealButtonState.set(ContentDisplay.LEFT);
		revealButtonDisabled.set(true);

		Task<Vault> revealTask = vaultService.createRevealTask(vault);
		revealTask.setOnSucceeded(evt -> {
			revealButtonState.set(ContentDisplay.TEXT_ONLY);
			revealButtonDisabled.set(false);
			window.close();
		});
		revealTask.setOnFailed(evt -> {
			LOG.warn("Reveal failed.", revealTask.getException());
			revealButtonState.set(ContentDisplay.TEXT_ONLY);
			revealButtonDisabled.set(false);
		});
		executor.execute(revealTask);
		if (rememberChoiceCheckbox.isSelected()) {
			vault.getVaultSettings().actionAfterUnlock.setValue(WhenUnlocked.REVEAL);
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ReadOnlyObjectProperty<ContentDisplay> revealButtonStateProperty() {
		return revealButtonState;
	}

	public ContentDisplay getRevealButtonState() {
		return revealButtonState.get();
	}

	public BooleanProperty revealButtonDisabledProperty() {
		return revealButtonDisabled;
	}

	public boolean isRevealButtonDisabled() {
		return revealButtonDisabled.get();
	}

}
