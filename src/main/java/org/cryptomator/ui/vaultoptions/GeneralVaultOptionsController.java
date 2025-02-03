package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.settings.WhenUnlocked;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;
import org.cryptomator.ui.health.HealthCheckComponent;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
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
	private final HealthCheckComponent.Builder healthCheckWindow;
	private final ResourceBundle resourceBundle;

	public TextField vaultName;
	public CheckBox unlockOnStartupCheckbox;
	public ChoiceBox<WhenUnlocked> actionAfterUnlockChoiceBox;
	public CheckBox lockAfterTimeCheckbox;
	public NumericTextField lockTimeInMinutesTextField;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, HealthCheckComponent.Builder healthCheckWindow, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.healthCheckWindow = healthCheckWindow;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		vaultName.textProperty().set(vault.getVaultSettings().displayName.get());
		vaultName.focusedProperty().addListener(this::trimVaultNameOnFocusLoss);
		vaultName.setTextFormatter(new TextFormatter<>(this::checkVaultNameLength));
		unlockOnStartupCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().unlockAfterStartup);
		actionAfterUnlockChoiceBox.getItems().addAll(WhenUnlocked.values());
		actionAfterUnlockChoiceBox.valueProperty().bindBidirectional(vault.getVaultSettings().actionAfterUnlock);
		actionAfterUnlockChoiceBox.setConverter(new WhenUnlockedConverter(resourceBundle));
		lockAfterTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().autoLockWhenIdle);
		Bindings.bindBidirectional(lockTimeInMinutesTextField.textProperty(), vault.getVaultSettings().autoLockIdleSeconds, new IdleTimeSecondsConverter());
	}

	private void trimVaultNameOnFocusLoss(Observable observable, Boolean wasFocussed, Boolean isFocussed) {
		var displayNameSetting = vault.getVaultSettings().displayName;
		if (!isFocussed) {
			var trimmed = vaultName.getText().trim();
			if (!trimmed.isEmpty()) {
				displayNameSetting.set(trimmed); //persist changes
			} else {
				vaultName.setText(displayNameSetting.get()); //revert changes
			}
		}
	}

	private TextFormatter.Change checkVaultNameLength(TextFormatter.Change change) {
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

	private static class IdleTimeSecondsConverter extends StringConverter<Number> {

		@Override
		public String toString(Number seconds) {
			int minutes = seconds.intValue() / 60; // int-truncate
			return Integer.toString(minutes);
		}

		@Override
		public Number fromString(String string) {
			try {
				int minutes = Integer.valueOf(string);
				return minutes * 60;
			} catch (NumberFormatException e) {
				return 0;
			}
		}
	}

	public void startHealthCheck() {
		healthCheckWindow.vault(vault).owner(window).build().showHealthCheckWindow();
	}
}
