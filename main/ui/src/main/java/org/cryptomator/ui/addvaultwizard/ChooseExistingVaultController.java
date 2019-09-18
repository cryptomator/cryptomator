package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChooseExistingVaultController.class);

	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final Lazy<Scene> successScene;
	private final ObjectProperty<Path> vaultPath;
	private final ObjectProperty<Vault> vault;
	private final VaultListManager vaultListManager;
	private final ResourceBundle resourceBundle;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, ObjectProperty<Path> vaultPath, @AddVaultWizard ObjectProperty<Vault> vault, VaultListManager vaultListManager, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.successScene = successScene;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
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
		File file = fileChooser.showOpenDialog(window);
		if (file != null) {
			vaultPath.setValue(file.toPath().toAbsolutePath().getParent());
			try {
				Vault newVault = vaultListManager.add(vaultPath.get());
				vault.set(newVault);
				window.setScene(successScene.get());
			} catch (NoSuchFileException e) {
				LOG.error("Nope", e);
				// TODO
			}
		}
	}

}
