package org.cryptomator.ui.addvaultwizard;

import com.tobiasdiez.easybind.EasyBind;
import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
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
	private final Lazy<Scene> chooseNameScene;
	private final Lazy<Scene> choosePasswordScene;
	private final ErrorComponent.Builder errorComponent;
	private final LocationPresets locationPresets;
	private final ObjectProperty<Path> vaultPath;
	private final StringProperty vaultName;
	private final ResourceBundle resourceBundle;
	private final BooleanBinding validVaultPath;
	private final BooleanProperty usePresetPath;
	private final StringProperty warningText;

	private Path customVaultPath = DEFAULT_CUSTOM_VAULT_PATH;
	public ToggleGroup predefinedLocationToggler;
	public RadioButton iclouddriveRadioButton;
	public RadioButton dropboxRadioButton;
	public RadioButton gdriveRadioButton;
	public RadioButton onedriveRadioButton;
	public RadioButton customRadioButton;

	@Inject
	CreateNewVaultLocationController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_NAME) Lazy<Scene> chooseNameScene, @FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD) Lazy<Scene> choosePasswordScene, ErrorComponent.Builder errorComponent, LocationPresets locationPresets, ObjectProperty<Path> vaultPath, @Named("vaultName") StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.chooseNameScene = chooseNameScene;
		this.choosePasswordScene = choosePasswordScene;
		this.errorComponent = errorComponent;
		this.locationPresets = locationPresets;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.resourceBundle = resourceBundle;
		this.validVaultPath = Bindings.createBooleanBinding(this::isValidVaultPath, vaultPath);
		this.usePresetPath = new SimpleBooleanProperty();
		this.warningText = new SimpleStringProperty();
	}

	private boolean isValidVaultPath() {
		return vaultPath.get() != null && Files.notExists(vaultPath.get());
	}

	@FXML
	public void initialize() {
		predefinedLocationToggler.selectedToggleProperty().addListener(this::togglePredefinedLocation);
		usePresetPath.bind(predefinedLocationToggler.selectedToggleProperty().isNotEqualTo(customRadioButton));
		EasyBind.subscribe(vaultPath, this::vaultPathDidChange);
	}

	private void vaultPathDidChange(Path newValue) {
		if ( newValue != null && !Files.notExists(newValue)) {
			warningText.set(resourceBundle.getString("addvaultwizard.new.fileAlreadyExists"));
		} else {
			warningText.set(null);
		}
	}

	private void togglePredefinedLocation(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		if (iclouddriveRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getIclouddriveLocation().resolve(vaultName.get()));
		} else if (dropboxRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getDropboxLocation().resolve(vaultName.get()));
		} else if (gdriveRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getGdriveLocation().resolve(vaultName.get()));
		} else if (onedriveRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getOnedriveLocation().resolve(vaultName.get()));
		} else if (customRadioButton.equals(newValue)) {
			vaultPath.set(customVaultPath.resolve(vaultName.get()));
		}
	}

	@FXML
	public void back() {
		window.setScene(chooseNameScene.get());
	}

	@FXML
	public void next() {
		try {
			// check if we have write access AND the vaultPath doesn't already exist:
			assert Files.isDirectory(vaultPath.get().getParent());
			Path createdDir = Files.createDirectory(vaultPath.get());
			Files.delete(createdDir); // assert: dir exists and is empty
			window.setScene(choosePasswordScene.get());
		} catch (FileAlreadyExistsException e) {
			LOG.warn("Can not use already existing vault path {}", vaultPath.get());
			warningText.set(resourceBundle.getString("addvaultwizard.new.fileAlreadyExists"));
		} catch (NoSuchFileException e) {
			LOG.warn("At least one path component does not exist of path {}", vaultPath.get());
			warningText.set(resourceBundle.getString("addvaultwizard.new.locationDoesNotExist"));
		} catch (IOException e) {
			LOG.error("Failed to create and delete directory at chosen vault path.", e);
			errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
		}
	}

	@FXML
	public void chooseCustomVaultPath() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("addvaultwizard.new.directoryPickerTitle"));
		if (Files.exists(customVaultPath)) {
			directoryChooser.setInitialDirectory(customVaultPath.toFile());
		} else {
			directoryChooser.setInitialDirectory(DEFAULT_CUSTOM_VAULT_PATH.toFile());
		}
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

	public BooleanBinding validVaultPathProperty() {
		return validVaultPath;
	}

	public Boolean getValidVaultPath() {
		return validVaultPath.get();
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

	public StringProperty warningTextProperty() {
		return warningText;
	}

	public String getWarningText() {
		return warningText.get();
	}

	public BooleanBinding showWarningProperty() {
		return warningText.isNotEmpty();
	}

	public boolean isShowWarning() {
		return showWarningProperty().get();
	}
}
