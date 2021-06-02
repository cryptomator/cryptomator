package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.util.converter.NumberStringConverter;

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
		lockAfterTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().lockAfterTime());
		Bindings.bindBidirectional(lockTimeInMinutesTextField.textProperty(), vault.getVaultSettings().lockTimeInMinutes(), new NumberStringConverter());
	}

}
