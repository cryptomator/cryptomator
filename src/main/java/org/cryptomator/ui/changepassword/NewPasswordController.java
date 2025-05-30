package org.cryptomator.ui.changepassword;

import org.cryptomator.common.Passphrase;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.controls.NiceSecurePasswordField;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.ResourceBundle;

public class NewPasswordController implements FxController {

	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final IntegerProperty passwordStrength = new SimpleIntegerProperty(-1);
	private final BooleanProperty goodPassword = new SimpleBooleanProperty();

	public NiceSecurePasswordField passwordField;
	public NiceSecurePasswordField reenterField;
	public Label passwordStrengthLabel;
	public FontAwesome5IconView passwordStrengthCheckmark;
	public FontAwesome5IconView passwordStrengthWarning;
	public FontAwesome5IconView passwordStrengthCross;
	public Label passwordMatchLabel;
	public FontAwesome5IconView passwordMatchCheckmark;
	public FontAwesome5IconView passwordMatchCross;

	public NewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater) {
		this.resourceBundle = resourceBundle;
		this.strengthRater = strengthRater;
	}

	@FXML
	public void initialize() {
		passwordStrength.bind(Bindings.createIntegerBinding(() -> strengthRater.computeRate(passwordField.getCharacters()), passwordField.textProperty()));

		passwordStrengthLabel.graphicProperty().bind(Bindings.createObjectBinding(this::getIconViewForPasswordStrengthLabel, passwordField.textProperty(), passwordStrength));
		passwordStrengthLabel.textProperty().bind(passwordStrength.map(strengthRater::getStrengthDescription));

		BooleanBinding passwordsMatch = Bindings.createBooleanBinding(this::passwordFieldsMatch, passwordField.textProperty(), reenterField.textProperty());
		BooleanBinding reenterFieldNotEmpty = reenterField.textProperty().isNotEmpty();
		passwordMatchLabel.visibleProperty().bind(reenterFieldNotEmpty);
		passwordMatchLabel.graphicProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(passwordMatchCheckmark).otherwise(passwordMatchCross));
		passwordMatchLabel.textProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(resourceBundle.getString("newPassword.passwordsMatch")).otherwise(resourceBundle.getString("newPassword.passwordsDoNotMatch")));

		BooleanBinding sufficientStrength = Bindings.createBooleanBinding(this::sufficientStrength, passwordField.textProperty());
		goodPassword.bind(passwordsMatch.and(sufficientStrength));
	}

	private FontAwesome5IconView getIconViewForPasswordStrengthLabel() {
		if (passwordField.getCharacters().length() == 0) {
			return null;
		} else if (passwordStrength.intValue() <= -1) {
			return passwordStrengthCross;
		} else if (passwordStrength.intValue() < 3) {
			return passwordStrengthWarning;
		} else {
			return passwordStrengthCheckmark;
		}
	}

	private boolean passwordFieldsMatch() {
		return CharSequence.compare(passwordField.getCharacters(), reenterField.getCharacters()) == 0;
	}

	private boolean sufficientStrength() {
		return strengthRater.fulfillsMinimumRequirements(passwordField.getCharacters());
	}

	/* Getter/Setter */

	public ReadOnlyBooleanProperty goodPasswordProperty() {
		return goodPassword;
	}

	public boolean isGoodPassword() {
		return goodPassword.get();
	}

	public IntegerProperty passwordStrengthProperty() {
		return passwordStrength;
	}

	public int getPasswordStrength() {
		return passwordStrength.get();
	}

	public Passphrase getNewPassword() {
		return passwordField.getCharacters();
	}

}
