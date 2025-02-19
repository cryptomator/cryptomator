package org.cryptomator.ui.wrongfilealert;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@WrongFileAlertScoped
public class WrongFileAlertController implements FxController {

	private static final String DOCUMENTATION_URI = "https://docs.cryptomator.org/desktop/accessing-vaults/";

	private final Application app;
	private final Stage window;

	private Image screenshot;

	@Inject
	public WrongFileAlertController(@WrongFileAlertWindow Stage window, Application app) {
		this.window = window;
		this.app = app;
	}

	@FXML
	public void initialize() {
		//TODO: add dark-mode screens
		final String resource = SystemUtils.IS_OS_MAC ? "/img/vault-volume-mac.png" : "/img/vault-volume-win.png";
		try (InputStream in = getClass().getResourceAsStream(resource)) {
			this.screenshot = new Image(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void openDocumentation() {
		app.getHostServices().showDocument(DOCUMENTATION_URI);
	}

	/* Getter */

	public Image getScreenshot() {
		return screenshot;
	}
}
