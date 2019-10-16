package org.cryptomator.ui.vaultoptions;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private final Vault vault;
	private final BooleanProperty osIsWindows = new SimpleBooleanProperty(SystemUtils.IS_OS_WINDOWS);
	private final BooleanBinding adapterIsDokan;
	private final ToggleGroup toggleGroup;
	public TextField driveName;
	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlags;
	public RadioButton automaticDriveLetter;
	public RadioButton specificDriveLetter;
	public RadioButton specificDirectory;

	@Inject
	MountOptionsController(@VaultOptionsWindow Vault vault, Settings settings) {
		this.vault = vault;
		this.adapterIsDokan = settings.preferredVolumeImpl().isEqualTo(VolumeImpl.DOKANY);
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
			//TODO: set any default free drive letter
		} else if (specificDirectory.isSelected()) {
			vault.getVaultSettings().usesIndividualMountPath().set(true);
			//TODO: open directory picker

		} else {
			//set property
			vault.getVaultSettings().usesIndividualMountPath().set(false);
			vault.getVaultSettings().winDriveLetter().set(null);
			vault.getVaultSettings().individualMountPath().set(null);
		}
	}

	@FXML
	public void selectEmptyDirectory(ActionEvent actionEvent) {
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

}
