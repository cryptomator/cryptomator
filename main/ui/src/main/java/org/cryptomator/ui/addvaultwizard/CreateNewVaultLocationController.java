package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultLocationController implements FxController {

	private final Stage window;
	private final Lazy<Scene> previousScene;
	private final ObjectProperty<Path> vaultPath;
	private final BooleanBinding vaultPathIsNull;
	private final StringProperty vaultName;
	private final ResourceBundle resourceBundle;

	//TODO: add parameter for next window
	@Inject
	CreateNewVaultLocationController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_NAME) Lazy<Scene> previousScene, ObjectProperty<Path> vaultPath, StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.previousScene = previousScene;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.resourceBundle = resourceBundle;
		this.vaultPathIsNull = vaultPath.isNull();
	}

	@FXML
	public void back() {
		window.setScene(previousScene.get());
	}

	@FXML
	public void next() {
		//TODO: what if there exists already a vault?
		if (hasFullAccessToLocation()) {
			window.close();
		} else {
			//TODO error handling
		}
	}

	private boolean hasFullAccessToLocation() {
		try {
			Path tmp = Files.createFile(vaultPath.get().resolve("tmp"));
			Files.delete(tmp);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@FXML
	public void chooseDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("addvaultwizard.new.directoryPickerTitle"));
		setInitialDirectory(directoryChooser);
		final File file = directoryChooser.showDialog(window);
		if (file != null) {
			vaultPath.setValue(file.toPath().toAbsolutePath());
		}
	}

	private void setInitialDirectory(DirectoryChooser chooser) {
		File userHome;
		try {
			userHome = new File(System.getProperty("user.home"));
		} catch (Exception e) {
			userHome = null;
		}
		if (userHome != null) {
			chooser.setInitialDirectory(userHome);
		}

	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultName;
	}

	public Path getVaultPath() {
		return vaultPath.get();
	}

	public ObjectProperty<Path> vaultPathProperty() {
		return vaultPath;
	}

	public boolean isVaultPathIsNull() {
		return vaultPathIsNull.get();
	}

	public BooleanBinding vaultPathIsNullProperty() {
		return vaultPathIsNull;
	}


}
