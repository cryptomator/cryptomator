package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.util.StringConverter;

@VaultOptionsScoped
public class AutoLockVaultOptionsController implements FxController {

	private final Vault vault;

	public CheckBox lockAfterTimeCheckbox;
	public NumericTextField lockTimeInMinutesTextField;

	@Inject
	AutoLockVaultOptionsController(@VaultOptionsWindow Vault vault) {
		this.vault = vault;
	}

	@FXML
	public void initialize() {
		lockAfterTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().autoLockWhenIdle());
		Bindings.bindBidirectional(lockTimeInMinutesTextField.textProperty(), vault.getVaultSettings().autoLockIdleSeconds(), new IdleTimeSecondsConverter());
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

}
