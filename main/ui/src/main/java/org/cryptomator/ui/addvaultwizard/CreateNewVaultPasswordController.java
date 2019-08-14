package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class CreateNewVaultPasswordController implements FxController {

	private final Stage window;
	private final Lazy<Scene> previousScene;
	private final StringProperty vaultName;
	private final ObjectProperty<Path> vaultPath;
	private final ObservableList<Vault> vaults;
	private final VaultFactory vaultFactory;
	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final IntegerProperty passwordStrength;

	public Button finishButton;
	public SecPasswordField passwordField;
	public SecPasswordField reenterField;
	public Region passwordStrengthLevel0;
	public Region passwordStrengthLevel1;
	public Region passwordStrengthLevel2;
	public Region passwordStrengthLevel3;
	public Region passwordStrengthLevel4;
	public Label passwordStrengthLabel;
	public HBox passwordMatchBox;
	public Rectangle checkmark;
	public Rectangle cross;
	public Label passwordMatchLabel;

	@Inject
	CreateNewVaultPasswordController(@AddVaultWizard Stage window, @FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION) Lazy<Scene> previousScene, StringProperty vaultName, ObjectProperty<Path> vaultPath, ObservableList<Vault> vaults, VaultFactory vaultFactory, ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater) {
		this.window = window;
		this.previousScene = previousScene;
		this.vaultName = vaultName;
		this.vaultPath = vaultPath;
		this.vaults = vaults;
		this.vaultFactory = vaultFactory;
		this.resourceBundle = resourceBundle;
		this.strengthRater = strengthRater;
		this.passwordStrength = new SimpleIntegerProperty(-1);
	}

	@FXML
	public void initialize() {
		//binds the actual strength value to the rating of the password util
		passwordStrength.bind(Bindings.createIntegerBinding(() -> strengthRater.computeRate(passwordField.getCharacters().toString()), passwordField.textProperty()));
		//binding indicating if the passwords not match
		BooleanBinding passwordsMatch = Bindings.createBooleanBinding(() -> CharSequence.compare(passwordField.getCharacters(), reenterField.getCharacters()) == 0, passwordField.textProperty(), reenterField.textProperty());
		BooleanBinding reenterFieldNotEmpty = reenterField.textProperty().isNotEmpty();
		//disable the finish button when passwords do not match or one is empty
		finishButton.disableProperty().bind(reenterFieldNotEmpty.not().or(passwordsMatch.not()));
		//make match indicator invisible when passwords do not match or one is empty
		passwordMatchBox.visibleProperty().bind(reenterFieldNotEmpty);
		checkmark.visibleProperty().bind(passwordsMatch.and(reenterFieldNotEmpty));
		cross.visibleProperty().bind(passwordsMatch.not().and(reenterFieldNotEmpty));
		passwordMatchLabel.textProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(resourceBundle.getString("addvaultwizard.new.passwordsMatch")).otherwise(resourceBundle.getString("addvaultwizard.new.passwordsDoNotMatch")));

		//bindsings for the password strength indicator
		passwordStrengthLevel0.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(0), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel1.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(1), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel2.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(2), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel3.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(3), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLevel4.backgroundProperty().bind(EasyBind.combine(passwordStrength, new SimpleIntegerProperty(4), strengthRater::getBackgroundWithStrengthColor));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	@FXML
	public void back() {
		window.setScene(previousScene.get());
	}

	@FXML
	public void finish() {
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path().setValue(vaultPath.get().resolve(vaultName.get()));
		Vault newVault = vaultFactory.get(vaultSettings);
		try {
			//TODO: why is creating the directory not part of the creation process?
			Files.createDirectory(vaultSettings.path().get());
			newVault.create(passwordField.getCharacters());
			vaults.add(newVault);
			window.close();
		} catch (IOException e) {
			e.printStackTrace();
			//TODO
		}
	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName.get();
	}

	public StringProperty vaultNameProperty() {
		return vaultName;
	}

}
