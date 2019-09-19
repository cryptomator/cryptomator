package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private final Vault vault;
	public TextField driveName;
	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlags;

	@Inject
	MountOptionsController(@VaultOptionsWindow Vault vault) {
		this.vault = vault;
	}

	@FXML
	public void initialize() {
		driveName.textProperty().bindBidirectional(vault.getVaultSettings().mountName());
		readOnlyCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().usesReadOnlyMode());
		mountFlags.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().not());
		readOnlyCheckbox.disableProperty().bind(customMountFlagsCheckbox.selectedProperty());

		customMountFlagsCheckbox.setSelected(vault.isHavingCustomMountFlags());
		if (vault.isHavingCustomMountFlags()) {
			mountFlags.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
		} else {
			mountFlags.textProperty().bind(vault.defaultMountFlagsProperty());
		}
	}

	@FXML
	public void toggleUseCustomMountFlags() {
		if (customMountFlagsCheckbox.isSelected()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
			mountFlags.textProperty().unbind();
			vault.setCustomMountFlags(vault.defaultMountFlagsProperty().get());
			mountFlags.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
		} else {
			mountFlags.textProperty().unbindBidirectional(vault.getVaultSettings().mountFlags());
			vault.setCustomMountFlags(null);
			mountFlags.textProperty().bind(vault.defaultMountFlagsProperty());
		}
	}
}
