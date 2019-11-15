package org.cryptomator.ui.preferences;

import com.auth0.jwt.interfaces.DecodedJWT;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.cryptomator.common.LicenseChecker;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import java.util.Optional;

@PreferencesScoped
public class RegistrationPreferencesController implements FxController {

	private final Settings settings;
	private final LicenseChecker licenseChecker;
	private final ObjectProperty<DecodedJWT> validJwtClaims;
	private final StringBinding licenseSubject;
	private final BooleanBinding registeredProperty;
	public TextArea registrationKeyField;

	@Inject
	RegistrationPreferencesController(Settings settings, LicenseChecker licenseChecker) {
		this.settings = settings;
		this.licenseChecker = licenseChecker;
		this.validJwtClaims = new SimpleObjectProperty<>();
		this.licenseSubject = Bindings.createStringBinding(this::getLicenseSubject, validJwtClaims);
		this.registeredProperty = validJwtClaims.isNotNull();
		
		Optional<DecodedJWT> claims = licenseChecker.check(settings.licenseKey().get());
		validJwtClaims.set(claims.orElse(null));
	}

	@FXML
	public void initialize() {
		registrationKeyField.textProperty().addListener(this::registrationKeyChanged);
	}

	private void registrationKeyChanged(@SuppressWarnings("unused") ObservableValue<? extends String> observable, @SuppressWarnings("unused") String oldValue, String newValue) {
		Optional<DecodedJWT> claims = licenseChecker.check(newValue);
		validJwtClaims.set(claims.orElse(null));
		if (claims.isPresent()) {
			settings.licenseKey().set(newValue);
		}
	}

	/* Observable Properties */
	
	public StringBinding licenseSubjectProperty() {
		return licenseSubject;
	}
	
	public String getLicenseSubject() {
		DecodedJWT claims = validJwtClaims.get();
		if (claims != null) {
			return claims.getSubject();
		} else {
			return null;
		}
	}

	public BooleanBinding registeredProperty() {
		return registeredProperty;
	}

	public boolean isRegistered() {
		return registeredProperty.get();
	}

}
