package org.cryptomator.common;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.cryptomator.common.settings.Settings;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.util.Optional;

@Singleton
public class LicenseHolder {

	private final Settings settings;
	private final LicenseChecker licenseChecker;
	private final ObjectProperty<DecodedJWT> validJwtClaims;
	private final StringBinding licenseSubject;
	private final BooleanBinding validLicenseProperty;

	@Inject
	public LicenseHolder(LicenseChecker licenseChecker, Settings settings) {
		this.settings = settings;
		this.licenseChecker = licenseChecker;
		this.validJwtClaims = new SimpleObjectProperty<>();
		this.licenseSubject = Bindings.createStringBinding(this::getLicenseSubject, validJwtClaims);
		this.validLicenseProperty = validJwtClaims.isNotNull();

		Optional<DecodedJWT> claims = licenseChecker.check(settings.licenseKey().get());
		validJwtClaims.set(claims.orElse(null));
	}

	public boolean validateAndStoreLicense(String licenseKey) {
		Optional<DecodedJWT> claims = licenseChecker.check(licenseKey);
		validJwtClaims.set(claims.orElse(null));
		if (claims.isPresent()) {
			settings.licenseKey().set(licenseKey);
			return true;
		} else {
			return false;
		}
	}

	/* Observable Properties */

	public Optional<String> getLicenseKey() {
		DecodedJWT claims = validJwtClaims.get();
		if (claims != null) {
			return Optional.of(claims.getToken());
		} else {
			return Optional.empty();
		}
	}

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

	public BooleanBinding validLicenseProperty() {
		return validLicenseProperty;
	}

	public boolean isValidLicense() {
		return validLicenseProperty.get();
	}

}
