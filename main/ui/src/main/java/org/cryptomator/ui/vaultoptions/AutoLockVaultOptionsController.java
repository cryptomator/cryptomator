package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ResourceBundle;

@VaultOptionsScoped
public class AutoLockVaultOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	public CheckBox lockOnSleepCheckbox;
	public CheckBox lockAfterIdleTimeCheckbox;
	public CheckBox lockAfterTimeCheckbox;
	public TextField lockIdleTimeInMinutesTextField;
	public TextField lockTimeInMinutesTextField;

	@Inject
	AutoLockVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void initialize() {
		lockOnSleepCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().lockOnSleep());
		lockAfterIdleTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().lockAfterIdleTime());
		lockIdleTimeInMinutesTextField.textProperty().bindBidirectional(vault.getVaultSettings().lockIdleTimeInMinutes());
		lockAfterTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().lockAfterTime());
		lockTimeInMinutesTextField.textProperty().bindBidirectional(vault.getVaultSettings().lockTimeInMinutes());
	}
}
