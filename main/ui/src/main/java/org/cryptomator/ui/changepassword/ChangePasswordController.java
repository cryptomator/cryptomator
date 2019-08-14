package org.cryptomator.ui.changepassword;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.util.PasswordStrengthUtil;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ResourceBundle;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final IntegerProperty passwordStrength;

	public SecPasswordField oldPasswordField;
	public SecPasswordField newPasswordField;
	public SecPasswordField reenterPasswordField;
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
	public void cancel() {
		window.close();
	}

	@FXML
	public void finish() {
		try {
			vault.changePassphrase(oldPasswordField.getCharacters(), newPasswordField.getCharacters());
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

}
