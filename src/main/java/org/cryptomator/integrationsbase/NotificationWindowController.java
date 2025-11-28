package org.cryptomator.integrationsbase;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class NotificationWindowController {

	public NotificationWindowController() {

	}

	@FXML
	public void initialize() {
		System.out.println("Hello");
	}

	public void handleButtonAction(ActionEvent actionEvent) {
		System.out.println("OKAY");
	}
}
