package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
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
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Files;
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

	//FXML
	public ToggleGroup predefinedLocationToggler;
	public RadioButton iclouddriveRadioButton;
	public RadioButton dropboxRadioButton;
	public RadioButton gdriveRadioButton;
	public RadioButton onedriveRadioButton;
	public RadioButton customRadioButton;
	public Label vaultPathStatus;
	public FontAwesome5IconView goodLocation;
	public FontAwesome5IconView badLocation;

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
		this.validVaultPath = Bindings.createBooleanBinding(this::isValidVaultPath, this.vaultPath);
		this.usePresetPath = new SimpleBooleanProperty();
		this.warningText = new SimpleStringProperty();
	}

	private boolean isValidVaultPath() {
		final Path p = vaultPath.get();
		if (p == null) {
			warningText.set("ERROR");
			return false;
		} else if (!Files.exists(p.getParent())) {
			warningText.set(resourceBundle.getString("addvaultwizard.new.locationDoesNotExist"));
			return false;
		} else if (!Files.isWritable(p.getParent())) {
			warningText.set(resourceBundle.getString("addvaultwizard.new.locationIsNotWritable"));
			return false;
		} else if (!Files.notExists(p)) {
			warningText.set(resourceBundle.getString("addvaultwizard.new.fileAlreadyExists"));
			return false;
		} else {
			warningText.set(null);
			return true;
		}
	}

	@FXML
	public void initialize() {
		predefinedLocationToggler.selectedToggleProperty().addListener(this::togglePredefinedLocation);
		usePresetPath.bind(predefinedLocationToggler.selectedToggleProperty().isNotEqualTo(customRadioButton));
		vaultPathStatus.graphicProperty().bind(Bindings.when(validVaultPath).then(goodLocation).otherwise(badLocation));
		vaultPathStatus.textProperty().bind(Bindings.createStringBinding(this::chooseStatusText, validVaultPath, warningText));
	}

	private String chooseStatusText() {
		if (validVaultPath.get()) {
			return resourceBundle.getString("addvaultwizard.new.locationIsOk");
		} else {
			return warningText.get();
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
		if (isValidVaultPath()) {
			window.setScene(choosePasswordScene.get());
		} else {
			//trigger change event
			var tmp = vaultPath.get();
			vaultPath.set(null);
			vaultPath.set(tmp);
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

	public BooleanBinding anyRadioButtonSelectedProperty() {
		return predefinedLocationToggler.selectedToggleProperty().isNotNull();
	}

	public boolean isAnyRadioButtonSelected() {
		return anyRadioButtonSelectedProperty().get();
	}
}
