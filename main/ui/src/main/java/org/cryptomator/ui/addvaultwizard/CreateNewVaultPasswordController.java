package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.SecPasswordField;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultPasswordController implements FxController {

	private final Stage window;
	private final Lazy<Scene> previousScene;
	private final StringProperty vaultName;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;

	public Button finishButton;
	public SecPasswordField passwordField;
	public SecPasswordField retypeField;

	@Inject
	CreateNewVaultPasswordController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> previousScene, StringProperty vaultName, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, VaultFactory vaultFactory, ResourceBundle resourceBundle) {
		this.window = window;
		this.previousScene = previousScene;
		this.vaultName = vaultName;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		finishButton.disableProperty().bind(passwordField.textProperty().isEmpty().or(passwordField.textProperty().isEqualTo(retypeField.textProperty()).not()));
	}

	@FXML
	public void back() {
		window.setScene(previousScene.get());
	}

	@FXML
	public void finish() {
		//VaultSettings vaultSettings = VaultSettings.withRandomId();
		//vaultSettings.path().setValue(vaultPath.get().resolve(vaultName.get()));
		//vaults.add(vaultFactory.get(vaultSettings));
		window.close();
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultName;
	}

}
