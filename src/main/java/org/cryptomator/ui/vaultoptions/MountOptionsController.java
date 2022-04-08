package org.cryptomator.ui.vaultoptions;

import com.google.common.base.Strings;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.WindowsDriveLetters;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
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
	private final VolumeImpl usedVolumeImpl;
	private final WindowsDriveLetters windowsDriveLetters;
	private final ResourceBundle resourceBundle;

	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlags;
	public ToggleGroup mountPoint;
	public RadioButton mountPointAuto;
	public RadioButton mountPointWinDriveLetter;
	public RadioButton mountPointCustomDir;
	public ChoiceBox<String> driveLetterSelection;

	@Inject
	MountOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, Settings settings, WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle, Environment environment) {
		this.window = window;
		this.vault = vault;
		this.usedVolumeImpl = settings.preferredVolumeImpl().get();
		this.windowsDriveLetters = windowsDriveLetters;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {

		// readonly:
		readOnlyCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().usesReadOnlyMode());
		//TODO: support this feature on Windows
		if (usedVolumeImpl == VolumeImpl.FUSE && isOsWindows()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
			readOnlyCheckbox.setDisable(true);
		}

		// custom mount flags:
		mountFlags.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().not());
		customMountFlagsCheckbox.setSelected(vault.isHavingCustomMountFlags());
		if (vault.isHavingCustomMountFlags()) {
			mountFlags.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
		} else {
			mountFlags.textProperty().bind(vault.defaultMountFlagsProperty());
		}

		// mount point options:
		mountPoint.selectedToggleProperty().addListener(this::toggleMountPoint);
		driveLetterSelection.getItems().addAll(windowsDriveLetters.getAllDriveLetters());
		driveLetterSelection.setConverter(new WinDriveLetterLabelConverter(windowsDriveLetters, resourceBundle));
		driveLetterSelection.setValue(vault.getVaultSettings().winDriveLetter().get());

		if (vault.getVaultSettings().useCustomMountPath().get() && vault.getVaultSettings().getCustomMountPath().isPresent()) {
			mountPoint.selectToggle(mountPointCustomDir);
		} else if (!Strings.isNullOrEmpty(vault.getVaultSettings().winDriveLetter().get())) {
			mountPoint.selectToggle(mountPointWinDriveLetter);
		} else {
			mountPoint.selectToggle(mountPointAuto);
		}

		vault.getVaultSettings().useCustomMountPath().bind(mountPoint.selectedToggleProperty().isEqualTo(mountPointCustomDir));
		vault.getVaultSettings().winDriveLetter().bind( //
				Bindings.when(mountPoint.selectedToggleProperty().isEqualTo(mountPointWinDriveLetter)) //
						.then(driveLetterSelection.getSelectionModel().selectedItemProperty()) //
						.otherwise((String) null) //
		);
	}

	@FXML
	public void toggleUseCustomMountFlags() {
		if (customMountFlagsCheckbox.isSelected()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
			mountFlags.textProperty().unbind();
			vault.setCustomMountFlags(vault.defaultMountFlagsProperty().get());
			mountFlags.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
		} else {
			mountFlags.textProperty().unbindBidirectional(vault.getVaultSettings().mountFlags());
			vault.setCustomMountFlags(null);
			mountFlags.textProperty().bind(vault.defaultMountFlagsProperty());
		}
	}

	@FXML
	public void chooseCustomMountPoint() {
		chooseCustomMountPointOrReset(mountPointCustomDir);
	}

	private void chooseCustomMountPointOrReset(Toggle previousMountToggle) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("vaultOptions.mount.mountPoint.directoryPickerTitle"));
		try {
			var initialDir = Path.of(vault.getVaultSettings().getCustomMountPath().orElse(System.getProperty("user.home")));

			if(Files.exists(initialDir)) {
				directoryChooser.setInitialDirectory(initialDir.toFile());
			}
		} catch (InvalidPathException e) {
			// no-op
		}
		File file = directoryChooser.showDialog(window);
		if (file != null) {
			vault.getVaultSettings().customMountPath().set(file.getAbsolutePath());
		} else {
			mountPoint.selectToggle(previousMountToggle);
		}
	}

	private void toggleMountPoint(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) {
		if (mountPointCustomDir.equals(newValue) && Strings.isNullOrEmpty(vault.getVaultSettings().customMountPath().get())) {
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

	public boolean isOsWindows() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	public boolean isCustomMountPointSupported() {
		return !(usedVolumeImpl == VolumeImpl.WEBDAV && isOsWindows());
	}

	public boolean isReadOnlySupported() {
		return !(usedVolumeImpl == VolumeImpl.FUSE && isOsWindows());
	}

	public StringProperty customMountPathProperty() {
		return vault.getVaultSettings().customMountPath();
	}

	public boolean isCustomMountOptionsSupported() {
		return usedVolumeImpl != VolumeImpl.WEBDAV;
	}

	public String getCustomMountPath() {
		return vault.getVaultSettings().customMountPath().get();
	}

}
