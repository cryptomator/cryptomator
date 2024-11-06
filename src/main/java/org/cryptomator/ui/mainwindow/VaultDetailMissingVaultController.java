package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.CustomDialog;
import org.cryptomator.ui.controls.FontAwesome5Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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


	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault, //
											 ObservableList<Vault> vaults, //
											 ResourceBundle resourceBundle, //
											 @MainWindow Stage window) {
		this.vault = vault;
		this.vaults = vaults;
		this.resourceBundle = resourceBundle;
		this.window = window;
	}

	@FXML
	public void recheck() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {

		new CustomDialog.Builder()
				.setOwner(window)
				.resourceBundle(resourceBundle)
				.titleKey("removeVault.title", vault.get().getDisplayName())
				.messageKey("removeVault.message")
				.descriptionKey("removeVault.description")
				.icon(FontAwesome5Icon.QUESTION)
				.okButtonKey("removeVault.confirmBtn")
				.cancelButtonKey("generic.button.cancel")
				.okAction(v -> {
					LOG.debug("Removing vault {}.", vault.get().getDisplayName());
					vaults.remove(vault.get());
					v.close();
				}) //
				.cancelAction(Stage::close) //
				.build();
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
