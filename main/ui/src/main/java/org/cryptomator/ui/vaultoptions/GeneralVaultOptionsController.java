package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.ResourceBundle;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private static final int VAULTNAME_TRUNCATE_THRESHOLD = 50;

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;

	public TextField vaultName;
	public CheckBox unlockOnStartupCheckbox;
	public ChoiceBox<WhenUnlocked> actionAfterUnlockChoiceBox;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		vaultName.textProperty().set(vault.getVaultSettings().displayName().get());
		vaultName.focusedProperty().addListener(this::trimVaultNameOnFocusLoss);
		vaultName.setTextFormatter(new TextFormatter<>(this::removeWhitespaces));
		unlockOnStartupCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().unlockAfterStartup());
		actionAfterUnlockChoiceBox.getItems().addAll(WhenUnlocked.values());
		actionAfterUnlockChoiceBox.valueProperty().bindBidirectional(vault.getVaultSettings().actionAfterUnlock());
		actionAfterUnlockChoiceBox.setConverter(new WhenUnlockedConverter(resourceBundle));
	}

	private void trimVaultNameOnFocusLoss(Observable observable, Boolean wasFocussed, Boolean isFocussed) {
		if (!isFocussed) {
			var trimmed = vaultName.getText().trim();
			vault.getVaultSettings().displayName().set(trimmed);
		}
	}

	private TextFormatter.Change removeWhitespaces(TextFormatter.Change change) {
		if (change.isContentChange() && change.getControlNewText().length() > VAULTNAME_TRUNCATE_THRESHOLD) {
			return null; // reject any change that would lead to a text exceeding threshold
		} else {
			return change;
		}
	}

	private static class WhenUnlockedConverter extends StringConverter<WhenUnlocked> {

		private final ResourceBundle resourceBundle;

		public WhenUnlockedConverter(ResourceBundle resourceBundle) {
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(WhenUnlocked obj) {
			return resourceBundle.getString(obj.getDisplayName());
		}

		@Override
		public WhenUnlocked fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

}
