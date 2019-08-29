package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final Lazy<Scene> successScene;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> vault;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, @AddVaultWizard ObjectProperty<Vault> vault, VaultFactory vaultFactory, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.successScene = successScene;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vault = vault;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void back() {
		window.setScene(welcomeScene.get());
	}

	@FXML
	public void chooseFileAndNext() {
		//TODO: error handling & cannot unlock added vault
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		final File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			vaultPath.setValue(file.toPath().toAbsolutePath().getParent());
			VaultSettings vaultSettings = VaultSettings.withRandomId();
			vaultSettings.path().setValue(vaultPath.get());
			Vault newVault = vaultFactory.get(vaultSettings);
			vaults.add(newVault);
			vault.set(newVault);
			//TODO: error handling?
			window.setScene(successScene.get());
		}
	}

}
