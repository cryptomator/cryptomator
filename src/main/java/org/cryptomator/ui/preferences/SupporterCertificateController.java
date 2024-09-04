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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import java.util.ResourceBundle;

@PreferencesScoped
public class SupporterCertificateController implements FxController {

	private static final String DONATE_URI = "https://cryptomator.org/donate";
	private static final String SPONSORS_URI = "https://cryptomator.org/sponsors";
	private static final String SUPPORTER_URI = "https://store.cryptomator.org/desktop";

	private final Application application;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	private final ResourceBundle resourceBundle;

	@FXML
	private TextArea supporterCertificateField;
	@FXML
	private Button trashButton;

	@Inject
	SupporterCertificateController(Application application, LicenseHolder licenseHolder, Settings settings, ResourceBundle resourceBundle) {
		this.application = application;
		this.licenseHolder = licenseHolder;
		this.settings = settings;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		supporterCertificateField.setText(licenseHolder.getLicenseKey().orElse(null));
		supporterCertificateField.textProperty().addListener(this::registrationKeyChanged);
		supporterCertificateField.setTextFormatter(new TextFormatter<>(this::removeWhitespaces));

		trashButton.setOnAction(_ -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle(resourceBundle.getString("removeLicenseKeyDialog.title"));
			alert.setHeaderText(resourceBundle.getString("removeLicenseKeyDialog.header"));
			alert.setContentText(resourceBundle.getString("removeLicenseKeyDialog.content"));

			alert.showAndWait().ifPresent(response -> {
				if (response == ButtonType.OK) {
					supporterCertificateField.setText(null);
					settings.licenseKey.set(null);
				}
			});
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

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}
}
