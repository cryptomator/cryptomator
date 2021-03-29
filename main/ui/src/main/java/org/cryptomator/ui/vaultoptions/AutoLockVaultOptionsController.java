package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ResourceBundle;

@VaultOptionsScoped
public class AutoLockVaultOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	public CheckBox lockAfterTimeCheckbox;
	public TextField lockTimeInMinutesTextField;

	@Inject
	AutoLockVaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void initialize() {
		lockAfterTimeCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().lockAfterTime());
		lockTimeInMinutesTextField.textProperty().bindBidirectional(vault.getVaultSettings().lockTimeInMinutes());
		//force the field to be a double with the correct decimal point
		lockTimeInMinutesTextField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d{0,9}([\\.]\\d{0,9})?")) {
					lockTimeInMinutesTextField.setText(newValue.replaceAll("[^\\d]", ""));
				}
			}
		});
	}
}
