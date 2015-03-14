package org.cryptomator.ui.controllers;

import javafx.application.Application;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import javax.inject.Inject;

public class MacWarningsController {

	@FXML
	private ListView<String> warningsList;

	private Stage stage;

	private final Application application;

	@Inject
	public MacWarningsController(Application application) {
		this.application = application;
	}

	@FXML
	private void didClickDismissButton(ActionEvent event) {
		stage.hide();
	}

	@FXML
	private void didClickMoreInformationButton(ActionEvent event) {
		application.getHostServices().showDocument("https://cryptomator.org/help.html#macWarning");
	}

	public void setMacWarnings(ObservableList<String> macWarnings) {
		this.warningsList.setItems(macWarnings);
		this.warningsList.getItems().addListener(new WeakListChangeListener<String>(this::warningsDidChange));
	}

	// closes this window automatically, if all warnings disappeared (e.g. due to an unmount event)
	private void warningsDidChange(Change<? extends String> change) {
		if (change.getList().isEmpty()) {
			stage.hide();
		}
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}
