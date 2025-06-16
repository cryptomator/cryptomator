package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;

@MainWindowScoped
public class VaultDetailMissingVaultController implements FxController {

	private final ObjectProperty<Vault> vault;
	private final ObservableList<Vault> vaults;
	private final ResourceBundle resourceBundle;
	private final Stage window;
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final Dialogs dialogs;

	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault, //
											 ObservableList<Vault> vaults, //
											 ResourceBundle resourceBundle, //
											 @MainWindow Stage window, //
											 Dialogs dialogs, //
											 RecoveryKeyComponent.Factory recoveryKeyWindow) {
		this.vault = vault;
		this.vaults = vaults;
		this.resourceBundle = resourceBundle;
		this.window = window;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.dialogs = dialogs;
	}

	@FXML
	public void recheck() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {
		dialogs.prepareRemoveVaultDialog(window, vault.get(), vaults).build().showAndWait();
	}

	@FXML
	void restoreVaultConfig() {
		if(KeyLoadingStrategy.isHubVault(vault.get().getVaultSettings().lastKnownKeyLoader.get())){
			dialogs.prepareContactHubAdmin(window).build().showAndWait();
		}
		else {
			recoveryKeyWindow.create(vault.get(), window, RecoveryActionType.RESTORE_VAULT_CONFIG).showOnboardingDialogWindow();
		}
	}

	@FXML
	void restoreMasterkey() {
		recoveryKeyWindow.create(vault.get(), window, RecoveryActionType.RESTORE_MASTERKEY).showRecoveryKeyRecoverWindow();
	}

	@FXML
	void changeLocation() {
		// copied from ChooseExistingVaultController class
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(resourceBundle.getString("addvaultwizard.existing.filePickerMimeDesc"), CRYPTOMATOR_FILENAME_GLOB));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			vault.get().getVaultSettings().path.setValue(masterkeyFile.toPath().toAbsolutePath().getParent());
			recheck();
		}
	}
}
