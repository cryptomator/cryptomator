package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultLocationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CreateNewVaultLocationController.class);
	private static final Path DEFAULT_CUSTOM_VAULT_PATH = Paths.get(System.getProperty("user.home"));

	private final Stage window;
	private final Lazy<Scene> previousScene;
	private final Lazy<Scene> nextScene;
	private final LocationPresets locationPresets;
	private final ObjectProperty<Path> vaultPath;
	private final BooleanBinding vaultPathIsNull;
	private final StringProperty vaultName;
	private final ResourceBundle resourceBundle;
	private final BooleanProperty usePresetPath;

	private Path customVaultPath = DEFAULT_CUSTOM_VAULT_PATH;
	public ToggleGroup predefinedLocationToggler;
	public RadioButton dropboxRadioButton;
	public RadioButton gdriveRadioButton;
	public RadioButton customRadioButton;

	//TODO: add parameter for next window

	@Inject
	CreateNewVaultLocationController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_NAME) Lazy<Scene> previousScene, @FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD) Lazy<Scene> nextScene, LocationPresets locationPresets, ObjectProperty<Path> vaultPath, StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.previousScene = previousScene;
		this.nextScene = nextScene;
		this.locationPresets = locationPresets;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.resourceBundle = resourceBundle;
		this.vaultPathIsNull = vaultPath.isNull();
		this.usePresetPath = new SimpleBooleanProperty();
	}

	@FXML
	public void initialize() {
		predefinedLocationToggler.selectedToggleProperty().addListener(this::togglePredefinedLocation);
		usePresetPath.bind(predefinedLocationToggler.selectedToggleProperty().isNotEqualTo(customRadioButton));
	}

	private void togglePredefinedLocation(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		if (dropboxRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getDropboxLocation().resolve(vaultName.get()));
		} else if (gdriveRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getGdriveLocation().resolve(vaultName.get()));
		} else if (customRadioButton.equals(newValue)) {
			vaultPath.set(customVaultPath.resolve(vaultName.get()));
		}
	}

	@FXML
	public void back() {
		window.setScene(previousScene.get());
	}

	@FXML
	public void next() {
		try {
			// check if we have write access AND the vaultPath doesn't already exist:
			assert Files.isDirectory(vaultPath.get().getParent());
			Path createdDir = Files.createDirectory(vaultPath.get());
			Files.delete(createdDir); // assert: dir exists and is empty
			window.setScene(nextScene.get());
		} catch (FileAlreadyExistsException e) {
			LOG.warn("Can not use already existing vault path: {}", vaultPath.get());
			// TODO show specific error text "vault can not be created at this path because some object already exists"
		} catch (NoSuchFileException | DirectoryNotEmptyException e) {
			LOG.error("Failed to delete recently created directory.", e);
			// TODO show generic error text for unexpected exception
		} catch (IOException e) {
			LOG.warn("Can not create vault at path: {}", vaultPath.get());
			// TODO show generic error text for unexpected exception
		}
	}

	@FXML
	public void chooseCustomVaultPath() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("addvaultwizard.new.directoryPickerTitle"));
		directoryChooser.setInitialDirectory(customVaultPath.toFile());
		final File file = directoryChooser.showDialog(window);
		if (file != null) {
			customVaultPath = file.toPath().toAbsolutePath();
			vaultPath.set(customVaultPath.resolve(vaultName.get()));
		}
	}

	/* Getter/Setter */

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

	public LocationPresets getLocationPresets() {
		return locationPresets;
	}

	public BooleanProperty usePresetPathProperty() {
		return usePresetPath;
	}

	public boolean getUsePresetPath() {
		return usePresetPath.get();
	}
}
