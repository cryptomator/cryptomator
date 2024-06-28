package org.cryptomator.ui.preferences;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NumericTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

@PreferencesScoped
public class NetworkPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(NetworkPreferencesController.class);

	public ToggleGroup proxyToggleGroup;
	public RadioButton noProxyBtn;
	public RadioButton systemSettingsBtn;
	public RadioButton manualProxyBtn;
	public VBox manualProxyBox;
	public NumericTextField httpProxyPort;
	public NumericTextField httpsProxyPort;

	@Inject
	NetworkPreferencesController() {
	}

	@FXML
	public void initialize() {
		proxyToggleGroup.selectToggle(noProxyBtn);
		proxyToggleGroup.selectedToggleProperty().addListener((_, _, newValue) -> {
			if (newValue != null) {
				RadioButton selectedRadioButton = (RadioButton) newValue;
				manualProxyBox.setDisable(!selectedRadioButton.equals(manualProxyBtn));
			}
		});
	}

}
