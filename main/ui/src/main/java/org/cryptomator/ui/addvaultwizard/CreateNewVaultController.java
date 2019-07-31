package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * TODO: Add trim() filter to vaultName
 */
@AddVaultWizardScoped
public class CreateNewVaultController implements FxController {

	public TextField textField;
	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final StringProperty vaultName;
	private final ResourceBundle resourceBundle;

	@Inject
	CreateNewVaultController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.vaultName = vaultName;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		vaultName.bind(textField.textProperty());
	}

	@FXML
	public void back() {
		window.setScene(welcomeScene.get());
	}

	@FXML
	public void next() {
		if (nameIsValid()) {
			window.close();
		} else {
			//TODO
		}
	}

	/**
	 * Checks if {@link CreateNewVaultController#vaultName}is a valid directory name in the OS by creating and deleting a directory with the giving name in the temporary section of the OS
	 * TODO: Logging
	 *
	 * @return true, if a directory with the name already exists or can be created
	 */
	private boolean nameIsValid() {
		try {
			Path tmp = Files.createTempDirectory(vaultName.get());
			Files.deleteIfExists(tmp.toAbsolutePath());
			return true;
		} catch (FileAlreadyExistsException e) {
			return true;
		} catch (IOException e) {
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultName;
	}

}
