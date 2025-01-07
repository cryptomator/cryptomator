package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.dialogs.SimpleDialog;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;

@MainWindowScoped
public class VaultDetailMissingVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultDetailMissingVaultController.class);

	private final ObjectProperty<Vault> vault;
	private final ObservableList<Vault> vaults;
	private final ResourceBundle resourceBundle;
	private final Stage window;
	private final Provider<SimpleDialog.Builder> simpleDialogProvider;

	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault, //
											 ObservableList<Vault> vaults, //
											 ResourceBundle resourceBundle, //
											 @MainWindow Stage window, //
											 Provider<SimpleDialog.Builder> simpleDialogProvider) {
		this.vault = vault;
		this.vaults = vaults;
		this.resourceBundle = resourceBundle;
		this.window = window;
		this.simpleDialogProvider = simpleDialogProvider;
	}

	@FXML
	public void recheck() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {
		Dialogs.showRemoveVaultDialog(simpleDialogProvider.get(),window,vault.get(),vaults);
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
