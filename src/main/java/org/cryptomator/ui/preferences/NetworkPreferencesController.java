package org.cryptomator.ui.preferences;

import org.cryptomator.common.settings.NetworkSettings;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;

import javax.inject.Inject;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@PreferencesScoped
public class NetworkPreferencesController implements FxController {

	private final Settings settings;

	public ToggleGroup proxyToggleGroup;
	public RadioButton noProxyBtn;
	public RadioButton systemSettingsBtn;
	public RadioButton manualProxyBtn;
	public VBox manualProxyBox;
	public HBox httpsProxyBox;

	@FXML
	public TextField httpProxy;
	public NumericTextField httpPort;
	public CheckBox samePortProxyForHttpHttps;
	public TextField httpsProxy;
	public NumericTextField httpsPort;

	@Inject
	NetworkPreferencesController(Settings settings) {
		this.settings = settings;
	}

	@FXML
	public void initialize() {

		initializeTextFieldWithValidation(httpProxy, settings.networkSettings.get().httpProxy);
		initializeTextFieldWithValidation(httpPort, settings.networkSettings.get().httpPort);
		initializeTextFieldWithValidation(httpsProxy, settings.networkSettings.get().httpsProxy);
		initializeTextFieldWithValidation(httpsPort, settings.networkSettings.get().httpsPort);

		samePortProxyForHttpHttps.selectedProperty().bindBidirectional(settings.networkSettings.get().samePortProxyForHttpHttps);
		httpsProxyBox.setDisable(settings.networkSettings.get().samePortProxyForHttpHttps.get());
		samePortProxyForHttpHttps.selectedProperty().addListener((_, _, newValue) -> {
			httpsProxyBox.setDisable(newValue);
		});

		switch (settings.networkSettings.get().mode.get()) {
			case NO -> {
				proxyToggleGroup.selectToggle(noProxyBtn);
				manualProxyBox.setDisable(true);
			}
			case SYSTEM -> {
				proxyToggleGroup.selectToggle(systemSettingsBtn);
				manualProxyBox.setDisable(true);
			}
			case MANUAL -> {
				proxyToggleGroup.selectToggle(manualProxyBtn);
				manualProxyBox.setDisable(false);
			}
		}

		proxyToggleGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
			if (newValue != null) {
				manualProxyBox.setDisable(!newValue.equals(manualProxyBtn));

				if (newValue.equals(noProxyBtn)) {
					settings.networkSettings.get().mode.set(NetworkSettings.ProxyMode.NO);
				} else if (newValue.equals(systemSettingsBtn)) {
					settings.networkSettings.get().mode.set(NetworkSettings.ProxyMode.SYSTEM);
				} else if (newValue.equals(manualProxyBtn)) {
					settings.networkSettings.get().mode.set(NetworkSettings.ProxyMode.MANUAL);
				}
			}
		});

	}

	private void initializeTextFieldWithValidation(TextField textField, StringProperty property) {
		textField.setText(property.get());

		textField.textProperty().addListener((_, _, newValue) -> {
			if (isValidHttpProxy(newValue)) {
				textField.setStyle("-fx-border-color: green; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-padding: 3px;");
			} else {
				textField.setStyle("-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-padding: 3px;");
			}
		});

		textField.focusedProperty().addListener((_, oldValue, newValue) -> {
			if (!newValue) {
				String text = textField.getText();
				if (isValidHttpProxy(text)) {
					property.set(text);
					textField.setStyle("");
				}
			}
		});
	}

	private boolean isValidHttpProxy(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		String regex = "^([\\w.-]+)$";
		return text.matches(regex);
	}

	private void initializeTextFieldWithValidation(TextField textField, IntegerProperty property) {
		textField.setText(String.valueOf(property.get()));

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (isValidPort(newValue)) {
				textField.setStyle("-fx-border-color: green; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-padding: 3px;");
			} else {
				textField.setStyle("-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-padding: 3px;");
			}
		});

		textField.focusedProperty().addListener((_, _, newValue) -> {
			if (!newValue) {
				String text = textField.getText();
				if (isValidPort(text)) {
					property.set(Integer.parseInt(text));
					textField.setText(removeLeadingZeros(textField.getText()));
					textField.setStyle("");
				}
			}
		});
	}

	public static String removeLeadingZeros(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.replaceFirst("^0+(?!$)", "");
	}

	private boolean isValidPort(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		try {
			int value = Integer.parseInt(text);
			return value >= 1024 && value <= 8888;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
