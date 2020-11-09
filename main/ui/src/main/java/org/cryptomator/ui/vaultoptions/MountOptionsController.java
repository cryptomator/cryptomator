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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * TODO: if WebDav is selected on a windows system, custom mount directory is _not_ supported. This is currently not indicated/shown/etc in the ui
 */
@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final BooleanProperty osIsWindows = new SimpleBooleanProperty(SystemUtils.IS_OS_WINDOWS);
	private final BooleanBinding webDavAndWindows;
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

	//FUSE + Windows -> Disable some (experimental) features for the user because they are unstable
	//Use argument Dfuse.experimental="true" to override
	private final BooleanBinding restrictToStableFuseOnWindows;

	@Inject
	MountOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, Settings settings, WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle, Environment environment) {
		this.window = window;
		this.vault = vault;
		this.webDavAndWindows = settings.preferredVolumeImpl().isEqualTo(VolumeImpl.WEBDAV).and(osIsWindows);
		this.windowsDriveLetters = windowsDriveLetters;
		this.resourceBundle = resourceBundle;

		BooleanBinding isFuseOnWindows = settings.preferredVolumeImpl().isEqualTo(VolumeImpl.FUSE).and(osIsWindows);
		this.restrictToStableFuseOnWindows = isFuseOnWindows.and(new SimpleBooleanProperty(!environment.useExperimentalFuse())); //Is FUSE on Win and is NOT experimental fuse enabled
	}

	@FXML
	public void initialize() {

		// readonly:
		readOnlyCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().usesReadOnlyMode());
		if (getRestrictToStableFuseOnWindows()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
		}
		readOnlyCheckbox.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().or(restrictToStableFuseOnWindows));

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

		if (vault.getVaultSettings().useCustomMountPath().get() && !getRestrictToStableFuseOnWindows() /* to prevent invalid states */) {
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
	private void chooseCustomMountPoint() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("vaultOptions.mount.mountPoint.directoryPickerTitle"));
		try {
			directoryChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
		} catch (Exception e) {
			//NO-OP
		}
		File file = directoryChooser.showDialog(window);
		if (file != null) {
			vault.getVaultSettings().customMountPath().set(file.getAbsolutePath());
		} else {
			vault.getVaultSettings().customMountPath().set(null);
		}
	}

	private void toggleMountPoint(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		if (mountPointCustomDir.equals(newValue) && Strings.isNullOrEmpty(vault.getVaultSettings().customMountPath().get())) {
			chooseCustomMountPoint();
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

	public BooleanProperty osIsWindowsProperty() {
		return osIsWindows;
	}

	public boolean getOsIsWindows() {
		return osIsWindows.get();
	}

	public BooleanBinding webDavAndWindowsProperty() {
		return webDavAndWindows;
	}

	public boolean isWebDavAndWindows() {
		return webDavAndWindows.get();
	}

	public StringProperty customMountPathProperty() {
		return vault.getVaultSettings().customMountPath();
	}

	public String getCustomMountPath() {
		return vault.getVaultSettings().customMountPath().get();
	}

	public Boolean getRestrictToStableFuseOnWindows() {
		return restrictToStableFuseOnWindows.get();
	}

}
