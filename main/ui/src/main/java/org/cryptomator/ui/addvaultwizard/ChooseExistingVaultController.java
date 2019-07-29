package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.model.VaultFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final BooleanProperty isVaultPathNull;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;

	@Inject
	ChooseExistingVaultController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, VaultFactory vaultFactory, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
		this.isVaultPathNull = new SimpleBooleanProperty();
		isVaultPathNull.bind(vaultPath.isNull());
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
		window.setScene(welcomeScene.get());
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

	public boolean getIsVaultPathNull() {
		return isVaultPathNull.get();
	}

	public BooleanProperty isVaultPathNullProperty() {
		return isVaultPathNull;
	}

}
