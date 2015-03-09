package org.cryptomator.ui.controllers;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class MacWarningsController {

	@FXML
	private ListView<String> warningsList;

	private Stage stage;

	@FXML
	private void hideWindow(ActionEvent event) {
		stage.hide();
	}

	public void setMacWarnings(ObservableList<String> macWarnings) {
		this.warningsList.setItems(macWarnings);
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}
