package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.regex.Pattern;

@AddVaultWizardScoped
public class CreateNewVaultNameController implements FxController {

	private static final Pattern VALID_NAME_PATTERN = Pattern.compile("[\\w -]+", Pattern.UNICODE_CHARACTER_CLASS);

	public TextField textField;
	private final Stage window;
	private final Lazy<Scene> chooseLocationScene;
	private final ObjectProperty<Path> vaultPath;
	private final StringProperty vaultName;
	private final BooleanBinding validVaultName;

	@Inject
	CreateNewVaultNameController(@AddVaultWizardWindow Stage window, //
								 @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> chooseLocationScene, //
								 ObjectProperty<Path> vaultPath, //
								 @Named("vaultName") StringProperty vaultName) {
		this.window = window;
		this.chooseLocationScene = chooseLocationScene;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.validVaultName = Bindings.createBooleanBinding(this::isValidVaultName, vaultName);
	}

	@FXML
	public void initialize() {
		vaultName.bindBidirectional(textField.textProperty());
		vaultName.addListener(this::vaultNameChanged);
	}

	private void vaultNameChanged(@SuppressWarnings("unused") Observable observable) {
		if (isValidVaultName()) {
			if (vaultPath.get() != null) {
				// update vaultPath if it is already set but the user went back to change its name:
				vaultPath.set(vaultPath.get().resolveSibling(vaultName.get()));
			}
		}
	}

	@FXML
	public void next() {
		window.setScene(chooseLocationScene.get());
		vaultName.set(vaultName.get().trim());
	}

	/* Getter/Setter */

	public BooleanBinding validVaultNameProperty() {
		return validVaultName;
	}

	public boolean isValidVaultName() {
		return vaultName.get() != null && VALID_NAME_PATTERN.matcher(vaultName.get().trim()).matches();
	}

}
