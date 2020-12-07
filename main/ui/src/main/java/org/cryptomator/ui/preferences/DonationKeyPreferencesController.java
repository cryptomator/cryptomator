package org.cryptomator.ui.preferences;

import com.google.common.base.CharMatcher;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;

@PreferencesScoped
public class DonationKeyPreferencesController implements FxController {

	private static final String DONATION_URI = "https://store.cryptomator.org/desktop";

	private final Application application;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	public TextArea donationKeyField;

	@Inject
	DonationKeyPreferencesController(Application application, LicenseHolder licenseHolder, Settings settings) {
		this.application = application;
		this.licenseHolder = licenseHolder;
		this.settings = settings;
	}

	@FXML
	public void initialize() {
		donationKeyField.setText(licenseHolder.getLicenseKey().orElse(null));
		donationKeyField.textProperty().addListener(this::registrationKeyChanged);
		donationKeyField.setTextFormatter(new TextFormatter<>(this::checkVaultNameLength));
	}

	private TextFormatter.Change checkVaultNameLength(TextFormatter.Change change) {
		if (change.isContentChange()) {
			var strippedText = CharMatcher.whitespace().removeFrom(change.getText());
			change.setText(strippedText);
		}
		return change;
	}

	private void registrationKeyChanged(@SuppressWarnings("unused") ObservableValue<? extends String> observable, @SuppressWarnings("unused") String oldValue, String newValue) {
		licenseHolder.validateAndStoreLicense(newValue);
		if (!licenseHolder.isValidLicense()) {
			settings.theme().set(UiTheme.LIGHT);
		}
	}

	@FXML
	public void getDonationKey() {
		application.getHostServices().showDocument(DONATION_URI);
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}
}
