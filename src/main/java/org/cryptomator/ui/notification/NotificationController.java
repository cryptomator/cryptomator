package org.cryptomator.ui.notification;

import org.cryptomator.common.Nullable;
import org.cryptomator.integrations.notify.NotifyService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.concurrent.ExecutorService;

@NotificationScoped
public class NotificationController implements FxController {

	private final String message;
	private final String description;
	private final NotifyAction callback;
	private final ExecutorService executorService;

	@FXML
	Button button;

	@Inject
	public NotificationController(@Named("Message") String message, @Named("Description") String description, @Nullable NotifyAction callback, ExecutorService executorService) {
		this.message = message;
		this.description = description;
		this.callback = callback;
		this.executorService = executorService;
	}


	//FXML bindings

	public String getMessage() {
		return message;
	}

	public String getDescription() {
		return description;
	}

	public String getButtonText() {
		return callback == null? "" : callback.label();
	}

	public boolean isButtonVisible() {
		return callback != null;
	}

	@FXML
	public void handleButtonAction(ActionEvent actionEvent) {
		executorService.submit(callback.action());
	}
}
