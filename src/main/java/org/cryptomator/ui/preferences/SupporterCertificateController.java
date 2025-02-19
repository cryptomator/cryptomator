package org.cryptomator.ui.preferences;

import com.google.common.base.CharMatcher;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.dialogs.Dialogs;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;


@PreferencesScoped
public class SupporterCertificateController implements FxController {

	private static final String DONATE_URI = "https://cryptomator.org/donate";
	private static final String SPONSORS_URI = "https://cryptomator.org/sponsors";
	private static final String SUPPORTER_URI = "https://store.cryptomator.org/desktop";

	private final Application application;
	private final Stage window;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	private final Dialogs dialogs;

	@FXML
	private TextArea supporterCertificateField;

	@Inject
	SupporterCertificateController(Application application, //
								   @PreferencesWindow Stage window,  //
								   LicenseHolder licenseHolder, //
								   Settings settings, //
								   Dialogs dialogs) {
		this.application = application;
		this.window = window;
		this.licenseHolder = licenseHolder;
		this.settings = settings;
		this.dialogs = dialogs;
	}

	@FXML
	public void initialize() {
		supporterCertificateField.setText(licenseHolder.getLicenseKey().orElse(null));
		supporterCertificateField.textProperty().addListener(this::registrationKeyChanged);
		supporterCertificateField.setTextFormatter(new TextFormatter<>(this::removeWhitespaces));
		settings.licenseKey.addListener((_, _, newValue) -> {
			if (newValue == null) {
				supporterCertificateField.setText(null);
			}
		});
	}

	private TextFormatter.Change removeWhitespaces(TextFormatter.Change change) {
		if (change.isContentChange()) {
			var strippedText = CharMatcher.whitespace().removeFrom(change.getText());
			change.setText(strippedText);
		}
		return change;
	}

	private void registrationKeyChanged(@SuppressWarnings("unused") ObservableValue<? extends String> observable, @SuppressWarnings("unused") String oldValue, String newValue) {
		licenseHolder.validateAndStoreLicense(newValue);
		if (!licenseHolder.isValidLicense()) {
			settings.theme.set(UiTheme.LIGHT);
		}
	}

	@FXML
	public void getSupporterCertificate() {
		application.getHostServices().showDocument(SUPPORTER_URI);
	}

	@FXML
	public void showDonate() {
		application.getHostServices().showDocument(DONATE_URI);
	}

	@FXML
	public void showSponsors() {
		application.getHostServices().showDocument(SPONSORS_URI);
	}

	@FXML
	void didClickRemoveCert() {
		dialogs.prepareRemoveCertDialog(window, settings).build().showAndWait();
	}

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}

}
