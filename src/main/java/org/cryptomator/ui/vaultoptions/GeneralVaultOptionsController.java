package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.Constants;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;
import org.cryptomator.ui.health.HealthCheckComponent;

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
	private final VaultSettings vaultSettings;
	private final HealthCheckComponent.Builder healthCheckWindow;
	private final ResourceBundle resourceBundle;

	public TextField vaultName;
	public CheckBox unlockOnStartupCheckbox;
	public CheckBox filenameLengthRestrictionCheckbox;
	public NumericTextField filenameLengthField;
	public ChoiceBox<WhenUnlocked> actionAfterUnlockChoiceBox;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, HealthCheckComponent.Builder healthCheckWindow, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.vaultSettings = vault.getVaultSettings();
		this.healthCheckWindow = healthCheckWindow;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		vaultName.textProperty().set(vaultSettings.displayName().get());
		vaultName.focusedProperty().addListener(this::trimAndSetVaultNameOnFocusLoss);
		vaultName.setTextFormatter(new TextFormatter<>(this::removeWhitespaces));

		unlockOnStartupCheckbox.selectedProperty().bindBidirectional(vaultSettings.unlockAfterStartup());

		actionAfterUnlockChoiceBox.getItems().addAll(WhenUnlocked.values());
		actionAfterUnlockChoiceBox.valueProperty().bindBidirectional(vaultSettings.actionAfterUnlock());
		actionAfterUnlockChoiceBox.setConverter(new WhenUnlockedConverter(resourceBundle));

		int maxClearTextFileNameLength = vaultSettings.maxCleartextFilenameLength().get();
		filenameLengthRestrictionCheckbox.selectedProperty().set(maxClearTextFileNameLength != Constants.UNKNOWN_CLEARTEXT_FILENAME_LENGTH_LIMIT && maxClearTextFileNameLength != Constants.UNLIMITED_CLEARTEXT_FILENAME_LENGTH);
		filenameLengthRestrictionCheckbox.selectedProperty().addListener(this::resetFileNameLimit);

		filenameLengthField.setText(maxClearTextFileNameLength);
		filenameLengthField.focusedProperty().addListener(this::setFileNameLengthLimitOnFocusLoss);
	}

	private void resetFileNameLimit(Observable observable, Boolean wasTicked, Boolean isTicked) {
		if (!isTicked) {
			vaultSettings.maxCleartextFilenameLength().set(Constants.UNKNOWN_CLEARTEXT_FILENAME_LENGTH_LIMIT);
			filenameLengthField.setText("");
		}
	}

	private void setFileNameLengthLimitOnFocusLoss(Observable observable, Boolean wasFocused, Boolean isFocused) {
		if (!isFocused) {
			vaultSettings.maxCleartextFilenameLength().set(filenameLengthField.getAsInt().orElse(Constants.UNLIMITED_CLEARTEXT_FILENAME_LENGTH));
		}
	}

	private void trimAndSetVaultNameOnFocusLoss(Observable observable, Boolean wasFocused, Boolean isFocused) {
		if (!isFocused) {
			var trimmed = vaultName.getText().trim();
			vaultSettings.displayName().set(trimmed);
		}
	}

	private TextFormatter.Change removeWhitespaces(TextFormatter.Change change) {
		if (change.isContentChange() && change.getControlNewText().length() > VAULTNAME_TRUNCATE_THRESHOLD) {
			return null; // reject any change that would lead to a text exceeding threshold
		} else {
			return change;
		}
	}

	@FXML
	public void showHealthCheck() {
		healthCheckWindow.vault(vault).build().showHealthCheckWindow();
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
