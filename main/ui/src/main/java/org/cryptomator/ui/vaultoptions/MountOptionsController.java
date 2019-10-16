package org.cryptomator.ui.vaultoptions;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.WindowsDriveLetters;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.Set;

@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final BooleanProperty osIsWindows = new SimpleBooleanProperty(SystemUtils.IS_OS_WINDOWS);
	private final BooleanBinding adapterIsDokan;
	private final WindowsDriveLetters windowsDriveLetters;
	private final ResourceBundle resourceBundle;
	private final ToggleGroup toggleGroup;
	public TextField driveName;
	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlags;
	public RadioButton automaticDriveLetter;
	public RadioButton specificDriveLetter;
	public RadioButton specificDirectory;
	public ChoiceBox<Path> driveLetterSelection;

	@Inject
	MountOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, Settings settings, WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.adapterIsDokan = settings.preferredVolumeImpl().isEqualTo(VolumeImpl.DOKANY);
		this.windowsDriveLetters = windowsDriveLetters;
		this.resourceBundle = resourceBundle;
		this.toggleGroup = new ToggleGroup();
	}

	@FXML
	public void initialize() {
		driveName.textProperty().bindBidirectional(vault.getVaultSettings().mountName());
		readOnlyCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().usesReadOnlyMode());
		mountFlags.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().not());
		readOnlyCheckbox.disableProperty().bind(customMountFlagsCheckbox.selectedProperty());

		customMountFlagsCheckbox.setSelected(vault.isHavingCustomMountFlags());
		if (vault.isHavingCustomMountFlags()) {
			mountFlags.textProperty().bindBidirectional(vault.getVaultSettings().mountFlags());
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
		} else {
			mountFlags.textProperty().bind(vault.defaultMountFlagsProperty());
		}

		toggleGroup.getToggles().addAll(automaticDriveLetter, specificDriveLetter, specificDirectory);
		initDriveLetterSelection();

		//TODO: set the toggleGroup correctly at init
	}

	private void initDriveLetterSelection() {
		driveLetterSelection.setConverter(new WinDriveLetterLabelConverter());
		Set<Path> freeLetters = windowsDriveLetters.getAvailableDriveLetters();
		driveLetterSelection.getItems().addAll(freeLetters);
		driveLetterSelection.getItems().sort(new WinDriveLetterComparator());
		chooseSelectedDriveLetter();
		//TODO: check if we should write only the letter or the path to the settings!!
		driveLetterSelection.getSelectionModel().selectedItemProperty().addListener(p -> vault.getVaultSettings().winDriveLetter().set(p.toString()));
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
	public void changeMountPointForWindows() {
		assert osIsWindows.get();
		if (specificDriveLetter.isSelected()) {
			vault.getVaultSettings().usesIndividualMountPath().set(true);
			vault.getVaultSettings().winDriveLetter().set(driveLetterSelection.getSelectionModel().getSelectedItem().toString());
			vault.getVaultSettings().individualMountPath().set(null);
		} else if (specificDirectory.isSelected()) {
			final File file = chooseDirectory();
			if (file != null) {
				//TODO: should we check wether the directory is empty or not?
				vault.getVaultSettings().usesIndividualMountPath().set(true);
				vault.getVaultSettings().individualMountPath().set(file.getAbsolutePath());
				vault.getVaultSettings().winDriveLetter().set(null);
			} else {
				//NO-OP
				//TODO: deduplicate code
				toggleGroup.selectToggle(automaticDriveLetter);
				vault.getVaultSettings().usesIndividualMountPath().set(false);
				vault.getVaultSettings().winDriveLetter().set(null);
				vault.getVaultSettings().individualMountPath().set(null);
			}
		} else {
			//set property
			vault.getVaultSettings().usesIndividualMountPath().set(false);
			vault.getVaultSettings().winDriveLetter().set(null);
			vault.getVaultSettings().individualMountPath().set(null);
		}
	}

	private File chooseDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("vaultOptions.mount.winDirChooser"));
		try {
			directoryChooser.setInitialDirectory(Path.of(System.getProperty("user.home")).toFile());
		} catch (Exception e) {
			//NO-OP
		}
		return directoryChooser.showDialog(window);
	}

	/**
	 * Converts 'C' to "C:" to translate between model and GUI.
	 */
	private class WinDriveLetterLabelConverter extends StringConverter<Path> {

		@Override
		public String toString(Path root) {
			if (root == null) {
				//TODO: none drive letter is selected
				return "";
			} else if (root.endsWith("occupied")) {
				return root.getRoot().toString().substring(0, 1) + " (" + resourceBundle.getString("vaultOptions.mount.winDriveLetterOccupied") + ")";
			} else {
				return root.toString().substring(0, 1);
			}
		}

		@Override
		public Path fromString(String string) {
			return Path.of(string);
		}

	}

	/**
	 * Natural sorting of ASCII letters, but <code>null</code> always on first, as this is "auto-assign".
	 */
	private static class WinDriveLetterComparator implements Comparator<Path> {

		@Override
		public int compare(Path c1, Path c2) {
			if (c1 == null) {
				return -1;
			} else if (c2 == null) {
				return 1;
			} else {
				return c1.compareTo(c2);
			}
		}
	}

	private void chooseSelectedDriveLetter() {
		assert SystemUtils.IS_OS_WINDOWS;
		// if the vault prefers a drive letter, that is currently occupied, this is our last chance to reset this:
		if (vault.getVaultSettings().winDriveLetter().isNotEmpty().get()) {
			final Path pickedRoot = Path.of(vault.getVaultSettings().winDriveLetter().get());
			if (windowsDriveLetters.getOccupiedDriveLetters().contains(pickedRoot)) {
				Path alteredPath = pickedRoot.resolve("occupied");
				driveLetterSelection.getItems().add(alteredPath);
				driveLetterSelection.getSelectionModel().select(alteredPath);
			} else {
				driveLetterSelection.getSelectionModel().select(pickedRoot);
			}
		} else {
			// first option is known to be 'auto-assign' due to #WinDriveLetterComparator.
			driveLetterSelection.getSelectionModel().selectFirst();
		}

	}

	// Getter & Setter

	public BooleanProperty osIsWindowsProperty() {
		return osIsWindows;
	}

	public boolean getOsIsWindows() {
		return osIsWindows.get();
	}

	public BooleanBinding adapterIsDokanProperty() {
		return adapterIsDokan;
	}

	public boolean getAdapterIsDokan() {
		return adapterIsDokan.get();
	}

	public StringProperty customMountPathProperty(){
		return vault.getVaultSettings().individualMountPath();
	}

	public String getCustomMountPath(){
		return vault.getVaultSettings().individualMountPath().get();
	}

}
