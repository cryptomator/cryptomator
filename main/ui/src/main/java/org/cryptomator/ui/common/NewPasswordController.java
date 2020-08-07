package org.cryptomator.ui.common;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.fxmisc.easybind.EasyBind;

import java.util.ResourceBundle;

public class NewPasswordController implements FxController {

	private final ResourceBundle resourceBundle;
	private final PasswordStrengthUtil strengthRater;
	private final ObjectProperty<CharSequence> password;
	private final IntegerProperty passwordStrength = new SimpleIntegerProperty(-1);

	public NiceSecurePasswordField passwordField;
	public NiceSecurePasswordField reenterField;
	public Label passwordStrengthLabel;
	public Label passwordMatchLabel;
	public FontAwesome5IconView checkmark;
	public FontAwesome5IconView warning;
	public FontAwesome5IconView cross;

	public NewPasswordController(ResourceBundle resourceBundle, PasswordStrengthUtil strengthRater, ObjectProperty<CharSequence> password) {
		this.resourceBundle = resourceBundle;
		this.strengthRater = strengthRater;
		this.password = password;
	}

	@FXML
	public void initialize() {
		passwordStrength.bind(Bindings.createIntegerBinding(() -> strengthRater.computeRate(passwordField.getCharacters()), passwordField.textProperty()));

		passwordStrengthLabel.graphicProperty().bind(Bindings.createObjectBinding(this::getIconViewForPasswordStrengthLabel, passwordField.textProperty(), passwordStrength));
		passwordStrengthLabel.textProperty().bind(EasyBind.map(passwordStrength, strengthRater::getStrengthDescription));

		BooleanBinding passwordsMatch = Bindings.createBooleanBinding(this::hasSamePasswordInBothFields, passwordField.textProperty(), reenterField.textProperty());
		BooleanBinding reenterFieldNotEmpty = reenterField.textProperty().isNotEmpty();
		passwordMatchLabel.visibleProperty().bind(reenterFieldNotEmpty);
		passwordMatchLabel.graphicProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(checkmark).otherwise(cross));
		passwordMatchLabel.textProperty().bind(Bindings.when(passwordsMatch.and(reenterFieldNotEmpty)).then(resourceBundle.getString("newPassword.passwordsMatch")).otherwise(resourceBundle.getString("newPassword.passwordsDoNotMatch")));

		passwordField.textProperty().addListener(this::passwordsDidChange);
		reenterField.textProperty().addListener(this::passwordsDidChange);
	}

	private FontAwesome5IconView getIconViewForPasswordStrengthLabel() {
		if (passwordField.getCharacters().length() == 0) {
			return null;
		} else if (passwordStrength.intValue() <= -1) {
			return cross;
		} else if (passwordStrength.intValue() < 3) {
			return warning;
		} else {
			return checkmark;
		}
	}

	private void passwordsDidChange(@SuppressWarnings("unused") Observable observable) {
		if (hasSamePasswordInBothFields() && strengthRater.fulfillsMinimumRequirements(passwordField.getCharacters())) {
			password.set(passwordField.getCharacters());
		} else {
			password.set("");
		}
	}

	private boolean hasSamePasswordInBothFields() {
		return CharSequence.compare(passwordField.getCharacters(), reenterField.getCharacters()) == 0;
	}

	/* Getter/Setter */

	public IntegerProperty passwordStrengthProperty() {
		return passwordStrength;
	}

	public int getPasswordStrength() {
		return passwordStrength.get();
	}

}
