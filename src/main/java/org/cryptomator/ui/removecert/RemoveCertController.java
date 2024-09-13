package org.cryptomator.ui.removecert;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@RemoveCertScoped
public class RemoveCertController implements FxController {

	private final Stage window;
	private final Settings settings;

	@Inject
	public RemoveCertController(@RemoveCertWindow Stage window, Settings settings) {
		this.window = window;
		this.settings = settings;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void remove() {
		settings.licenseKey.set(null);
		window.close();
	}
}
