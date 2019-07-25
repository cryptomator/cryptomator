package org.cryptomator.ui.addvaultwizard;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private final Stage window;
	private final FXMLLoaderFactory fxmlLoaders;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @AddVaultWizard FXMLLoaderFactory fxmlLoaders, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, VaultFactory vaultFactory, ResourceBundle resourceBundle) {
		this.window = window;
		this.fxmlLoaders = fxmlLoaders;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void chooseFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			vaultPath.setValue(file.toPath().toAbsolutePath().getParent());
		}
	}

	@FXML
	public void back() {
		fxmlLoaders.setScene("/fxml/addvault_welcome.fxml", window);
	}

	@FXML
	public void finish() {
		//TODO: error handling & cannot unlock added vault
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path().setValue(vaultPath.get());
		vaults.add(vaultFactory.get(vaultSettings));
		window.close();
	}

	/* Getter/Setter */

	public Path getVaultPath() {
		return vaultPath.get();
	}

	public ObjectProperty<Path> vaultPathProperty() {
		return vaultPath;
	}
}
