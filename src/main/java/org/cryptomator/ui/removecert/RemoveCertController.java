package org.cryptomator.ui.removecert;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@RemoveCertScoped
public class RemoveCertController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(org.cryptomator.ui.removecert.RemoveCertController.class);

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
