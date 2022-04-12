package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultLocationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(CreateNewVaultLocationController.class);
	private static final Path DEFAULT_CUSTOM_VAULT_PATH = Paths.get(System.getProperty("user.home"));
	private static final String TEMP_FILE_FORMAT = ".locationTest.cryptomator.tmp";

	private final Stage window;
	private final Lazy<Scene> chooseNameScene;
	private final Lazy<Scene> choosePasswordScene;
	private final LocationPresets locationPresets;
	private final ObjectProperty<Path> vaultPath;
	private final StringProperty vaultName;
	private final ResourceBundle resourceBundle;
	private final BooleanBinding validVaultPath;
	private final BooleanProperty usePresetPath;
	private final StringProperty statusText;
	private final ObjectProperty<Node> statusGraphic;

	private Path customVaultPath = DEFAULT_CUSTOM_VAULT_PATH;

	//FXML
	public ToggleGroup predefinedLocationToggler;
	public RadioButton iclouddriveRadioButton;
	public RadioButton dropboxRadioButton;
	public RadioButton gdriveRadioButton;
	public RadioButton onedriveRadioButton;
	public RadioButton megaRadioButton;
	public RadioButton pcloudRadioButton;
	public RadioButton customRadioButton;
	public Label vaultPathStatus;
	public FontAwesome5IconView goodLocation;
	public FontAwesome5IconView badLocation;

	@Inject
	CreateNewVaultLocationController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_NAME) Lazy<Scene> chooseNameScene, @FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD) Lazy<Scene> choosePasswordScene, LocationPresets locationPresets, ObjectProperty<Path> vaultPath, @Named("vaultName") StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.chooseNameScene = chooseNameScene;
		this.choosePasswordScene = choosePasswordScene;
		this.locationPresets = locationPresets;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.resourceBundle = resourceBundle;
		this.validVaultPath = Bindings.createBooleanBinding(this::validateVaultPathAndSetStatus, this.vaultPath);
		this.usePresetPath = new SimpleBooleanProperty();
		this.statusText = new SimpleStringProperty();
		this.statusGraphic = new SimpleObjectProperty<>();
	}

	private boolean validateVaultPathAndSetStatus() {
		final Path p = vaultPath.get();
		if (p == null) {
			statusText.set("Error: Path is NULL.");
			statusGraphic.set(badLocation);
			return false;
		} else if (!Files.exists(p.getParent())) {
			statusText.set(resourceBundle.getString("addvaultwizard.new.locationDoesNotExist"));
			statusGraphic.set(badLocation);
			return false;
		} else if (!isActuallyWritable(p.getParent())) {
			statusText.set(resourceBundle.getString("addvaultwizard.new.locationIsNotWritable"));
			statusGraphic.set(badLocation);
			return false;
		} else if (!Files.notExists(p)) {
			statusText.set(resourceBundle.getString("addvaultwizard.new.fileAlreadyExists"));
			statusGraphic.set(badLocation);
			return false;
		} else {
			statusText.set(resourceBundle.getString("addvaultwizard.new.locationIsOk"));
			statusGraphic.set(goodLocation);
			return true;
		}
	}

	private boolean isActuallyWritable(Path p) {
		Path tmpFile = p.resolve(TEMP_FILE_FORMAT);
		try (var chan = Files.newByteChannel(tmpFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE)) {
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				Files.deleteIfExists(tmpFile);
			} catch (IOException e) {
				LOG.warn("Unable to delete temporary file {}. Needs to be deleted manually.", tmpFile);
			}
		}
	}

	@FXML
	public void initialize() {
		predefinedLocationToggler.selectedToggleProperty().addListener(this::togglePredefinedLocation);
		usePresetPath.bind(predefinedLocationToggler.selectedToggleProperty().isNotEqualTo(customRadioButton));
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
		} else if (megaRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getMegaLocation().resolve(vaultName.get()));
		} else if (pcloudRadioButton.equals(newValue)) {
			vaultPath.set(locationPresets.getPcloudLocation().resolve(vaultName.get()));
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
		if (validateVaultPathAndSetStatus()) {
			window.setScene(choosePasswordScene.get());
		} else {
			validVaultPath.invalidate();
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

	public StringProperty statusTextProperty() {
		return statusText;
	}

	public String getStatusText() {
		return statusText.get();
	}

	public ObjectProperty<Node> statusGraphicProperty() {
		return statusGraphic;
	}

	public Node getStatusGraphic() {
		return statusGraphic.get();
	}
}
