package org.cryptomator.ui.quit;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.desktop.QuitResponse;
import java.util.concurrent.ExecutorService;

@QuitScoped
public class QuitController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(QuitController.class);

	private final Stage window;
	private final QuitResponse response;
	private final ExecutorService executor;
	private final ObjectProperty<ContentDisplay> quitButtonState;

	@Inject
	QuitController(@QuitWindow Stage window, QuitResponse response, ExecutorService executor) {
		this.window = window;
		this.response = response;
		this.executor = executor;
		this.quitButtonState = new SimpleObjectProperty<>(ContentDisplay.TEXT_ONLY);
	}

	@FXML
	public void cancel() {
		LOG.info("Quitting application canceled by user.");
		window.close();
		response.cancelQuit();
	}

	@FXML
	public void quit() {
		LOG.warn("Quit not yet implemented.");
		window.close();
		response.cancelQuit();
	}

	@FXML
	public void forceQuit() {
		LOG.warn("Force Quit not yet implemented.");
		window.close();
		response.cancelQuit();
	}

	/* Observable Properties */

	public ObjectProperty<ContentDisplay> quitButtonStateProperty() {
		return quitButtonState;
	}

	public ContentDisplay getQuitButtonState() {
		return quitButtonState.get();
	}
}
