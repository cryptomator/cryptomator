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
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

@AddVaultWizardScoped
public class CreateNewVaultNameController implements FxController {

	private static final Pattern VALID_NAME_PATTERN = Pattern.compile("[\\w -]+", Pattern.UNICODE_CHARACTER_CLASS);

	public TextField textField;
	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final Lazy<Scene> chooseLocationScene;
	private final ObjectProperty<Path> vaultPath;
	private final StringProperty vaultName;
	private final BooleanBinding validVaultName;
	private final BooleanBinding invalidVaultName;
	private final StringBinding warningText;

	@Inject
	CreateNewVaultNameController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> chooseLocationScene, ObjectProperty<Path> vaultPath, @Named("vaultName") StringProperty vaultName, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.chooseLocationScene = chooseLocationScene;
		this.vaultPath = vaultPath;
		this.vaultName = vaultName;
		this.validVaultName = Bindings.createBooleanBinding(this::isValidVaultName, vaultName);
		this.invalidVaultName = validVaultName.not();
		this.warningText = Bindings.when(vaultName.isNotEmpty().and(invalidVaultName)).then(resourceBundle.getString("addvaultwizard.new.invalidName")).otherwise((String) null);
	}

	@FXML
	public void initialize() {
		vaultName.bind(textField.textProperty());
		vaultName.addListener(this::vaultNameChanged);
	}

	public boolean isValidVaultName() {
		return vaultName.get() != null && VALID_NAME_PATTERN.matcher(vaultName.get().trim()).matches();
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
	public void back() {
		window.setScene(welcomeScene.get());
	}

	@FXML
	public void next() {
		window.setScene(chooseLocationScene.get());
	}

	/* Getter/Setter */

	public BooleanBinding invalidVaultNameProperty() {
		return invalidVaultName;
	}

	public boolean isInvalidVaultName() {
		return invalidVaultName.get();
	}

	public StringBinding warningTextProperty() {
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
