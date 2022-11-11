package org.cryptomator.ui.vaultoptions;

import com.google.common.base.Strings;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.mount.WindowsDriveLetters;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.Set;

@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final WindowsDriveLetters windowsDriveLetters;
	private final ResourceBundle resourceBundle;

	private final ObservableValue<Boolean> mountpointDirSupported;
	private final ObservableValue<Boolean> mountpointDriveLetterSupported;
	private final ObservableValue<Boolean> mountpointParentSupported; //TODO: use it in GUI
	private final ObservableValue<Boolean> readOnlySupported;
	private final ObservableValue<Boolean> mountFlagsSupported;

	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlagsField;
	public ToggleGroup mountPointToggleGroup;
	public RadioButton mountPointAutoBtn;
	public RadioButton mountPointDriveLetterBtn;
	public RadioButton mountPointDirBtn;
	public ChoiceBox<String> driveLetterSelection;

	@Inject
	MountOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, ObservableValue<MountService> mountService, WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle, Environment environment) {
		this.window = window;
		this.vault = vault;
		this.windowsDriveLetters = windowsDriveLetters;
		this.resourceBundle = resourceBundle;
		this.mountpointDirSupported = mountService.map(s -> s.hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR));
		this.mountpointDriveLetterSupported = mountService.map(s -> s.hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER));
		this.mountpointParentSupported = mountService.map(s -> s.hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT));
		this.mountFlagsSupported = mountService.map(s -> s.hasCapability(MountCapability.MOUNT_FLAGS));
		this.readOnlySupported = mountService.map(s -> s.hasCapability(MountCapability.READ_ONLY));
	}

	@FXML
	public void initialize() {
		// readonly:
		readOnlyCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().usesReadOnlyMode());

		// custom mount flags:
		mountFlagsField.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().not());
		customMountFlagsCheckbox.setSelected(vault.isHavingCustomMountFlags());

		// mount point options:
		mountPointToggleGroup.selectedToggleProperty().addListener(this::toggleMountPoint);
		driveLetterSelection.getItems().addAll(windowsDriveLetters.getAllDriveLetters());
		driveLetterSelection.setConverter(new WinDriveLetterLabelConverter(windowsDriveLetters, resourceBundle));
		driveLetterSelection.setValue(vault.getVaultSettings().winDriveLetter().get());

		if (vault.getVaultSettings().useCustomMountPath().get() && vault.getVaultSettings().getCustomMountPath().isPresent()) {
			mountPointToggleGroup.selectToggle(mountPointDirBtn);
		} else if (!Strings.isNullOrEmpty(vault.getVaultSettings().winDriveLetter().get())) {
			mountPointToggleGroup.selectToggle(mountPointDriveLetterBtn);
		} else {
			mountPointToggleGroup.selectToggle(mountPointAutoBtn);
		}

		vault.getVaultSettings().useCustomMountPath().bind(mountPointToggleGroup.selectedToggleProperty().isEqualTo(mountPointDirBtn));
		vault.getVaultSettings().winDriveLetter().bind( //
				Bindings.when(mountPointToggleGroup.selectedToggleProperty().isEqualTo(mountPointDriveLetterBtn)) //
						.then(driveLetterSelection.getSelectionModel().selectedItemProperty()) //
						.otherwise((String) null) //
		);
	}

	@FXML
	public void toggleUseCustomMountFlags() {
		if (customMountFlagsCheckbox.isSelected()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
			mountFlagsField.textProperty().unbind();
			vault.setCustomMountFlags(vault.defaultMountFlagsProperty().getValue());
			mountFlagsField.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
		} else {
			mountFlagsField.textProperty().unbindBidirectional(vault.getVaultSettings().mountFlags());
			vault.setCustomMountFlags(null);
			mountFlagsField.textProperty().bind(vault.defaultMountFlagsProperty());
		}
	}

	@FXML
	public void chooseCustomMountPoint() {
		chooseCustomMountPointOrReset(mountPointDirBtn);
	}

	private void chooseCustomMountPointOrReset(Toggle previousMountToggle) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("vaultOptions.mount.mountPoint.directoryPickerTitle"));
		try {
			var initialDir = Path.of(vault.getVaultSettings().getCustomMountPath().orElse(System.getProperty("user.home")));

			if (Files.exists(initialDir)) {
				directoryChooser.setInitialDirectory(initialDir.toFile());
			}
		} catch (InvalidPathException e) {
			// no-op
		}
		File file = directoryChooser.showDialog(window);
		if (file != null) {
			vault.getVaultSettings().customMountPath().set(file.getAbsolutePath());
		} else {
			mountPointToggleGroup.selectToggle(previousMountToggle);
		}
	}

	private void toggleMountPoint(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
		if (mountPointDirBtn.equals(newValue) && Strings.isNullOrEmpty(vault.getVaultSettings().customMountPath().get())) {
			chooseCustomMountPointOrReset(oldValue);
		}
	}

	/**
	 * Converts 'C' to "C:" to translate between model and GUI.
	 */
	private static class WinDriveLetterLabelConverter extends StringConverter<String> {

		private final Set<String> occupiedDriveLetters;
		private final ResourceBundle resourceBundle;

		WinDriveLetterLabelConverter(WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle) {
			this.occupiedDriveLetters = windowsDriveLetters.getOccupiedDriveLetters();
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(String driveLetter) {
			if (Strings.isNullOrEmpty(driveLetter)) {
				return "";
			} else if (occupiedDriveLetters.contains(driveLetter)) {
				return driveLetter + ": (" + resourceBundle.getString("vaultOptions.mount.winDriveLetterOccupied") + ")";
			} else {
				return driveLetter + ":";
			}
		}

		@Override
		public String fromString(String string) {
			throw new UnsupportedOperationException();
		}

	}

	// Getter & Setter

	public ObservableValue<Boolean> mountFlagsSupportedProperty() {
		return mountFlagsSupported;
	}

	public boolean isMountFlagsSupported() {
		return mountFlagsSupported.getValue();
	}

	public ObservableValue<Boolean> mountpointDirSupportedProperty() {
		return mountpointDirSupported;
	}

	public boolean isMountpointDirSupported() {
		return mountpointDirSupported.getValue();
	}

	public ObservableValue<Boolean> mountpointParentSupportedProperty() {
		return mountpointParentSupported;
	}

	public boolean isMountpointParentSupported() {
		return mountpointParentSupported.getValue();
	}

	public ObservableValue<Boolean> mountpointDriveLetterSupportedProperty() {
		return mountpointDriveLetterSupported;
	}

	public boolean isMountpointDriveLetterSupported() {
		return mountpointDriveLetterSupported.getValue();
	}

	public ObservableValue<Boolean> readOnlySupportedProperty() {
		return mountpointDriveLetterSupported;
	}

	public boolean isReadOnlySupported() {
		return readOnlySupported.getValue();
	}


	public String getCustomMountPath() {
		return vault.getVaultSettings().customMountPath().get();
	}

}
