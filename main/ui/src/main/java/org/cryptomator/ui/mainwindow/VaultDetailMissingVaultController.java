package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.removevault.RemoveVaultComponent;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ResourceBundle;

@MainWindowScoped
public class VaultDetailMissingVaultController implements FxController {

	private final ObjectProperty<Vault> vault;
	private final RemoveVaultComponent.Builder removeVault;
	private final ResourceBundle resourceBundle;
	private final Stage window;


	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault, RemoveVaultComponent.Builder removeVault, ResourceBundle resourceBundle, @MainWindow Stage window) {
		this.vault = vault;
		this.removeVault = removeVault;
		this.resourceBundle = resourceBundle;
		this.window = window;
	}

	@FXML
	public void recheck() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {
		removeVault.vault(vault.get()).build().showRemoveVault();
	}

	@FXML
	void changeLocation() {
		// copied from ChooseExistingVaultController class
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			vault.get().getVaultSettings().path().setValue(masterkeyFile.toPath().toAbsolutePath().getParent());
			recheck();
		}
	}
}
