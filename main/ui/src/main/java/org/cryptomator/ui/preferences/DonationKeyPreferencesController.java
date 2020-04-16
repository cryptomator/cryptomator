package org.cryptomator.ui.preferences;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@PreferencesScoped
public class DonationKeyPreferencesController implements FxController {
	
	private static final String DONATION_URI = "https://cryptomator.org/store/desktop/";

	private final Application application;
	private final LicenseHolder licenseHolder;
	public TextArea donationKeyField;

	@Inject
	DonationKeyPreferencesController(Application application, LicenseHolder licenseHolder) {
		this.application = application;
		this.licenseHolder = licenseHolder;
	}

	@FXML
	public void initialize() {
		donationKeyField.setText(licenseHolder.getLicenseKey().orElse(null));
		donationKeyField.textProperty().addListener(this::registrationKeyChanged);
	}

	private void registrationKeyChanged(@SuppressWarnings("unused") ObservableValue<? extends String> observable, @SuppressWarnings("unused") String oldValue, String newValue) {
		licenseHolder.validateAndStoreLicense(newValue);
	}

	@FXML
	public void getDonationKey() {
		application.getHostServices().showDocument(DONATION_URI);
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}
}
