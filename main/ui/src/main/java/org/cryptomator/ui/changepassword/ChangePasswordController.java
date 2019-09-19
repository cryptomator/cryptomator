package org.cryptomator.ui.changepassword;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.common.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ResourceBundle;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final IntegerProperty passwordStrength;

	public NiceSecurePasswordField oldPasswordField;
	public NiceSecurePasswordField newPasswordField;
	public NiceSecurePasswordField reenterPasswordField;
	public Label passwordStrengthLabel;
	public HBox passwordMatchBox;
	public FontAwesome5IconView checkmark;
	public FontAwesome5IconView cross;
	public Label passwordMatchLabel;
	public CheckBox finalConfirmationCheckbox;
	public Button finishButton;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
		this.strengthRater = strengthRater;
		this.passwordStrength = new SimpleIntegerProperty(-1);
	}

	@FXML
	public void initialize() {
		//binds the actual strength value to the rating of the password util
		passwordStrength.bind(Bindings.createIntegerBinding(() -> strengthRater.computeRate(newPasswordField.getCharacters().toString()), newPasswordField.textProperty()));
		//binding indicating if the passwords not match
		BooleanBinding passwordsMatch = Bindings.createBooleanBinding(() -> CharSequence.compare(newPasswordField.getCharacters(), reenterPasswordField.getCharacters()) == 0, newPasswordField.textProperty(), reenterPasswordField.textProperty());
		BooleanBinding reenterFieldNotEmpty = reenterPasswordField.textProperty().isNotEmpty();
		//disable the finish button when passwords do not match or one is empty
		finishButton.disableProperty().bind(reenterFieldNotEmpty.not().or(passwordsMatch.not()).or(finalConfirmationCheckbox.selectedProperty().not()));
		//make match indicator invisible when passwords do not match or one is empty
		passwordMatchBox.visibleProperty().bind(reenterFieldNotEmpty);
		checkmark.visibleProperty().bind(passwordsMatch.and(reenterFieldNotEmpty));
		checkmark.managedProperty().bind(checkmark.visibleProperty());
		cross.visibleProperty().bind(passwordsMatch.not().and(reenterFieldNotEmpty));
		cross.managedProperty().bind(cross.visibleProperty());
		passwordMatchLabel.textProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(resourceBundle.getString("changepassword.passwordsMatch")).otherwise(resourceBundle.getString("changepassword.passwordsDoNotMatch")));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void finish() {
		try {
			CryptoFileSystemProvider.changePassphrase(vault.getPath(), MASTERKEY_FILENAME, oldPasswordField.getCharacters(), newPasswordField.getCharacters());
			LOG.info("Successful changed password for {}", vault.getDisplayableName());
			window.close();
		} catch (IOException e) {
			//TODO
			LOG.error("IO error occured during password change. Unable to perform operation.", e);
			e.printStackTrace();
		} catch (InvalidPassphraseException e) {
			//TODO
			LOG.info("Wrong old password.");
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public IntegerProperty passwordStrengthProperty() {
		return passwordStrength;
	}

	public int getPasswordStrength() {
		return passwordStrength.get();
	}
}
